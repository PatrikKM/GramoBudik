package cz.patrik.gramobudik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val prefs = AlarmPreferences(context)
        if (prefs.isScheduled &&
            !prefs.songUri.isNullOrBlank() &&
            AlarmScheduler.canScheduleExactAlarms(context)
        ) {
            try {
                AlarmScheduler.schedule(context, prefs.hour, prefs.minute)
            } catch (_: SecurityException) {
                prefs.isScheduled = false
                prefs.scheduledTriggerMillis = 0L
            }
        }
    }
}
