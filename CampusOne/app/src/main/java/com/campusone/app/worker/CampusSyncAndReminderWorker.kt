package com.campusone.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.campusone.app.CampusOneApp
import com.campusone.app.domain.usecase.ReminderSchedulerLogic

/**
 * Periodic / boot-triggered on-device reminder verification worker.
 * Iterates over strongly typed Kotlin Lists (zero JSONArray / ambiguous iterator() calls).
 */
class CampusSyncAndReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? CampusOneApp ?: return Result.success()
            val dao = app.container.database.studyAndCampusDao()
            val now = System.currentTimeMillis()

            val upcomingExams = dao.getAllExamsOnce()
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
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
