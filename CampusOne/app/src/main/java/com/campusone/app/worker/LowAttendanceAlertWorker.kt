package com.campusone.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campusone.app.CampusOneApp
import com.campusone.app.MainActivity
import com.campusone.app.domain.usecase.AttendanceCalculator
import java.util.concurrent.TimeUnit

class LowAttendanceAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as? CampusOneApp ?: return Result.success()
            val profile = app.container.userPreferencesRepository.getProfileOnce()
            if (!profile.lowAttendanceAlertEnabled) return Result.success()

            val subjects = app.container.database.coreDao().getAllSubjectsOnce()
            val threshold = profile.lowAttendanceAlertThreshold.coerceIn(50, 95)

            val atRisk = subjects.filter { sub ->
                val target = if (sub.targetPercentage in 50..95) sub.targetPercentage else threshold
                AttendanceCalculator.shouldTriggerLowAttendanceAlert(
                    attended = sub.attendedCount,
                    totalConducted = sub.totalClassesCount,
                    thresholdPercent = target
                )
            }

            if (atRisk.isNotEmpty()) {
                val worst = atRisk.minByOrNull {
                    if (it.totalClassesCount > 0) {
                        (it.attendedCount.toDouble() * 100.0) / it.totalClassesCount.toDouble()
                    } else {
                        100.0
                    }
                } ?: atRisk.first()

                val effectiveTarget = if (worst.targetPercentage in 50..95) worst.targetPercentage else threshold
                val advice = AttendanceCalculator.evaluate(
                    attended = worst.attendedCount,
                    totalConducted = worst.totalClassesCount,
                    targetPercentage = effectiveTarget
                )

                val body = if (!advice.isAboveTarget) {
                    "${worst.code} is at ${advice.currentPercentage}% (below ${effectiveTarget}%). Attend next ${advice.classesMustAttendToRecover} classes to recover."
                } else {
                    "${worst.code} is at ${advice.currentPercentage}% (${advice.safeBunksAvailable} safe bunks left before ${effectiveTarget}%)."
                }

                postLowAttendanceNotification(
                    context = applicationContext,
                    subjectId = worst.id,
                    title = "Low Attendance Alert: ${worst.name}",
                    body = body
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun postLowAttendanceNotification(
        context: Context,
        subjectId: String,
        title: String,
        body: String
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Low Attendance Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                nm?.createNotificationChannel(channel)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_route", "attendance")
                putExtra("open_subject_id", subjectId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                subjectId.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val CHANNEL_ID = "campusone_low_attendance_alerts"
        private const val WORK_NAME_DAILY = "campus_daily_low_attendance_check"
        private const val WORK_NAME_IMMEDIATE = "campus_immediate_low_attendance_check"
        private const val NOTIFICATION_ID = 4041

        fun scheduleDailyCheck(context: Context) {
            try {
                val periodic = PeriodicWorkRequestBuilder<LowAttendanceAlertWorker>(24, TimeUnit.HOURS)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_DAILY,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodic
                )
            } catch (_: Exception) {
            }
        }

        fun triggerImmediateCheck(context: Context) {
            try {
                val oneTime = OneTimeWorkRequestBuilder<LowAttendanceAlertWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_IMMEDIATE,
                    ExistingWorkPolicy.REPLACE,
                    oneTime
                )
            } catch (_: Exception) {
            }
        }
    }
}
