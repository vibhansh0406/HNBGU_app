package com.campusone.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class CampusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            try {
                val req = OneTimeWorkRequestBuilder<CampusSyncAndReminderWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "campus_boot_reschedule",
                    ExistingWorkPolicy.KEEP,
                    req
                )
            } catch (_: Exception) {
            }
        }
    }
}
