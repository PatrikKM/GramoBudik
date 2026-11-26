package cz.patrik.gramobudik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = AlarmPreferences(context)
        val alarmId = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
        val alarm = if (alarmId > 0) {
            prefs.getAlarm(alarmId)
        } else {
            prefs.getAlarms().firstOrNull { !it.songUri.isNullOrBlank() }
        } ?: return

        val songUri = alarm.songUri
        if (songUri.isNullOrBlank()) {
            prefs.upsertAlarm(alarm.copy(isEnabled = false, nextTriggerMillis = 0L))
            return
        }

        if (alarm.repeatDays.isEmpty()) {
            prefs.upsertAlarm(alarm.copy(isEnabled = false, nextTriggerMillis = 0L))
        } else if (AlarmScheduler.canScheduleExactAlarms(context)) {
            try {
                AlarmScheduler.schedule(context, alarm)
            } catch (_: SecurityException) {
                prefs.upsertAlarm(alarm.copy(isEnabled = false, nextTriggerMillis = 0L))
            }
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG,
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }

        try {
            PlaybackService.startPlayback(
                context = context,
                songUri = songUri,
                songName = alarm.songName.takeUnless { it.isNullOrBlank() } ?: DEFAULT_SONG_NAME,
                openRingingUi = true,
                vibrate = alarm.vibrate,
            )
        } catch (t: Throwable) {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            throw t
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val WAKE_LOCK_TAG = "gramobudik:alarm_receiver"
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
        private const val DEFAULT_SONG_NAME = "Vybraná písnička"
    }
}
