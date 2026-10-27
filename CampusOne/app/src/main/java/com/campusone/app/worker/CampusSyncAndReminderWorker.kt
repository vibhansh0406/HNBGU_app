package com.campusone.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campusone.app.CampusOneApp
import com.campusone.app.domain.usecase.ReminderSchedulerLogic
import com.campusone.app.domain.usecase.TimetableEngine
import java.time.LocalDate
import java.time.LocalTime

class CampusSyncAndReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? CampusOneApp ?: return Result.success()
            val studyDao = app.container.database.studyAndCampusDao()
            val coreDao = app.container.database.coreDao()
            val profile = app.container.userPreferencesRepository.getProfileOnce()
            val now = System.currentTimeMillis()

            val upcomingExams = studyDao.getAllExamsOnce()
            for (exam in upcomingExams) {
                val specs = ReminderSchedulerLogic.computeReminderSpecs(
                    itemId = exam.id,
                    targetEpochMillis = exam.examEpochMillis,
                    nowEpochMillis = now
                )
                for (spec in specs) {
                    ClassReminderWorker.scheduleOneTimeReminder(
                        context = applicationContext,
                        uniqueWorkName = spec.uniqueWorkName,
                        title = "Upcoming Exam: ${exam.subjectName}",
                        message = "${exam.examTitle} at ${exam.venue} (${exam.timeSlot})",
                        delayMillis = spec.initialDelayMillis
                    )
                }
            }

            val todayDow = LocalDate.now().dayOfWeek.value
            val nowMins = LocalTime.now().let { it.hour * 60 + it.minute }
            val slots = coreDao.getAllTimetableSlotsOnce()
            for (slot in slots) {
                val delayMs = TimetableEngine.computeNextOccurrenceDelayMillis(
                    dayOfWeek = slot.dayOfWeek,
                    startTime = slot.startTime,
                    leadMinutes = profile.reminderLeadMinutes,
                    currentDayOfWeek = todayDow,
                    currentMinutesOfDay = nowMins
                )
                ClassReminderWorker.scheduleOneTimeReminder(
                    context = applicationContext,
                    uniqueWorkName = "class_rem_${slot.id}",
                    title = "Upcoming Class: ${slot.subjectName}",
                    message = "${slot.startTime} - ${slot.endTime} in ${slot.room} (${slot.teacherName})",
                    delayMillis = delayMs
                )
            }

            LowAttendanceAlertWorker.scheduleDailyCheck(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
