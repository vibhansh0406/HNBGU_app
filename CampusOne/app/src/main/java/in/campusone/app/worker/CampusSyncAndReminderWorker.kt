package `in`.campusone.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import `in`.campusone.app.data.local.dao.CampusLifeDao
import `in`.campusone.app.data.local.dao.StudyDao
import `in`.campusone.app.data.local.dao.TimetableDao
import `in`.campusone.app.data.preferences.UserPreferencesRepository
import `in`.campusone.app.data.repository.CoreCampusRepository
import `in`.campusone.app.domain.model.TimetableSlot
import `in`.campusone.app.domain.usecase.CampusLifeEngine
import `in`.campusone.app.domain.usecase.ReminderSchedulerLogic
import `in`.campusone.app.domain.usecase.TimetableEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class CampusSyncAndReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CoreCampusRepository,
    private val timetableDao: TimetableDao,
    private val studyDao: StudyDao,
    private val campusLifeDao: CampusLifeDao,
    private val prefs: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val profile = prefs.userProfile.first()
        val nowMillis = System.currentTimeMillis()

        // 1. Check upcoming class reminders offline using local Room timetable
        val entities = timetableDao.getTimetableSnapshot(profile.sectionKey)
        val slots = entities.map {
            TimetableSlot(
                id = it.id,
                subjectId = it.subjectId,
                subjectName = it.subjectName,
                subjectCode = it.subjectCode,
                teacherName = it.teacherName,
                roomNumber = it.roomNumber,
                dayOfWeek = it.dayOfWeek,
                startTime = it.startTime,
                endTime = it.endTime,
                isElective = it.isElective
            )
        }
        val nextClass = TimetableEngine.findNextOrOngoingClass(slots, LocalDateTime.now())
        if (nextClass != null && !nextClass.isOngoing && nextClass.isToday) {
            if (nextClass.minutesUntilStart in 1..profile.reminderLeadMinutes.toLong()) {
                sendNotification(
                    channelId = "campusone_class_reminders",
                    channelName = "Class Reminders",
                    notificationId = nextClass.slot.subjectName.hashCode(),
                    title = "Upcoming Class in ${nextClass.minutesUntilStart} mins",
                    body = "${nextClass.slot.subjectName} • ${nextClass.slot.roomNumber}"
                )
            }
        }

        // 2. Check Assignment Deadlines (1 day before = 1440m, 2 hours before = 120m)
        val pendingAssignments = studyDao.getPendingAssignmentsSnapshot()
        for (task in pendingAssignments) {
            if (!task.reminderEnabled) continue
            val offsets = buildList {
                if (task.remind1DayBefore) add(1440)
                if (task.remind2HoursBefore) add(120)
            }
            val specs = ReminderSchedulerLogic.computeReminderSpecs(
                targetId = task.id,
                title = task.title,
                dueEpochMillis = task.dueDateEpochMillis,
                nowEpochMillis = nowMillis,
                isCompleted = task.isCompleted,
                reminderEnabled = task.reminderEnabled,
                leadOffsetsMinutes = offsets
            )
            for (spec in specs) {
                if (spec.initialDelayMillis in 1..(15 * 60_000L)) {
                    val label = if (spec.leadOffsetMinutes >= 1440) "Due Tomorrow" else "Due in 2 Hours"
                    sendNotification(
                        channelId = "campusone_assignment_reminders",
                        channelName = "Assignment & Exam Deadlines",
                        notificationId = spec.uniqueWorkName.hashCode(),
                        title = "Assignment $label: ${task.subjectTag}",
                        body = task.title
                    )
                }
            }
        }

        // 3. Check Exam Reminders (24 hours & 3 hours before exam)
        val upcomingExams = studyDao.getExamsSnapshot()
        for (exam in upcomingExams) {
            if (!exam.reminderEnabled) continue
            val specs = ReminderSchedulerLogic.computeReminderSpecs(
                targetId = exam.id,
                title = exam.subjectName,
                dueEpochMillis = exam.examDateEpochMillis,
                nowEpochMillis = nowMillis,
                isCompleted = false,
                reminderEnabled = exam.reminderEnabled,
                leadOffsetsMinutes = ReminderSchedulerLogic.DEFAULT_EXAM_OFFSETS_MINS
            )
            for (spec in specs) {
                if (spec.initialDelayMillis in 1..(15 * 60_000L)) {
                    sendNotification(
                        channelId = "campusone_assignment_reminders",
                        channelName = "Assignment & Exam Deadlines",
                        notificationId = spec.uniqueWorkName.hashCode(),
                        title = "Upcoming Exam: ${exam.subjectCode}",
                        body = "${exam.subjectName} • ${exam.examDateFormatted} (${exam.roomOrCenter})"
                    )
                }
            }
        }

        // 4. Phase 3: Check Campus Event Reminders (for GOING / INTERESTED events) & 30-Day Lost & Found Auto-Archive
        val rsvpedEvents = campusLifeDao.getRsvpedEventsSnapshot()
        for (event in rsvpedEvents) {
            val specs = ReminderSchedulerLogic.computeReminderSpecs(
                targetId = event.id,
                title = event.title,
                dueEpochMillis = event.eventEpochMillis,
                nowEpochMillis = nowMillis,
                isCompleted = false,
                reminderEnabled = true,
                leadOffsetsMinutes = listOf(event.reminderLeadMinutes)
            )
            for (spec in specs) {
                if (spec.initialDelayMillis in 1..(15 * 60_000L)) {
                    sendNotification(
                        channelId = "campusone_event_reminders",
                        channelName = "Campus Event Reminders",
                        notificationId = spec.uniqueWorkName.hashCode(),
                        title = "Campus Event Starting Soon: ${event.clubName}",
                        body = "${event.title} • ${event.venue}"
                    )
                }
            }
        }

        // Run 30-day Lost & Found auto-archive cleanup
        val archiveCutoff = nowMillis - CampusLifeEngine.AUTO_ARCHIVE_WINDOW_MILLIS
        campusLifeDao.autoArchiveOldLostFound(archiveCutoff)

        // 5. Perform Firestore Delta Sync if network is available
        runCatching {
            repository.performDeltaSync()
        }

        return Result.success()
    }

    private fun sendNotification(
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        private const val WORK_NAME = "campusone_periodic_sync_and_reminders"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CampusSyncAndReminderWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
