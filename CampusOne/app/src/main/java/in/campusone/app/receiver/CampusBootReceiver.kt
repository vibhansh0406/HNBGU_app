package `in`.campusone.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.campusone.app.worker.CampusSyncAndReminderWorker

class CampusBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            "android.intent.action.QUICKBOOT_POWERON" == intent.action
        ) {
            // Reschedule reboot-safe reminders & delta sync after device reboot
            CampusSyncAndReminderWorker.schedule(context)
        }
    }
}
