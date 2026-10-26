package `in`.campusone.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.campusone.app.worker.CampusReminderWorker

class CampusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            CampusReminderWorker.schedule(context)
        }
    }
}
