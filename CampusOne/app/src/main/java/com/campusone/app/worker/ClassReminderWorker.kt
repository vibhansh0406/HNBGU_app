package com.campusone.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ClassReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "CampusOne Reminder"
        val message = inputData.getString(KEY_MESSAGE) ?: "Upcoming class or academic deadline"
        showNotification(applicationContext, title, message)
        return Result.success()
    }

    private fun showNotification(context: Context, title: String, message: String) {
        try {
            val channelId = "campusone_reminders"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    channelId,
                    "Class & Deadline Reminders",
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

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() and 0xFFFFFFF).toInt(), notification)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val KEY_TITLE = "key_title"
        const val KEY_MESSAGE = "key_message"

        fun scheduleOneTimeReminder(
            context: Context,
            uniqueWorkName: String,
            title: String,
            message: String,
            delayMillis: Long
        ) {
            try {
                val request = OneTimeWorkRequestBuilder<ClassReminderWorker>()
                    .setInitialDelay(delayMillis.coerceAtLeast(1_000L), TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            KEY_TITLE to title,
                            KEY_MESSAGE to message
                        )
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (_: Exception) {
            }
        }
    }
}

