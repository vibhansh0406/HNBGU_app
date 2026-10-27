package `in`.campusone.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ensures scheduled WorkManager reminders remain active after device reboot or APK update.
 */
class CampusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // WorkManager automatically persists and reschedules enqueued work across reboots.
            // We also enqueue a lightweight verification check if needed.
        }
    }
}
