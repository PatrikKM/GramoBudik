package cz.patrik.gramobudik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = AlarmPreferences(context)
        val songUri = prefs.songUri
        val songName = prefs.songName

        prefs.isScheduled = false
        prefs.scheduledTriggerMillis = 0L

        if (songUri.isNullOrBlank()) {
            return
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
                songName = songName.takeUnless { it.isNullOrBlank() } ?: DEFAULT_SONG_NAME,
                openRingingUi = true,
            )
        } catch (t: Throwable) {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            throw t
        }
    }

    private companion object {
        const val WAKE_LOCK_TAG = "gramobudik:alarm_receiver"
        const val WAKE_LOCK_TIMEOUT_MS = 10_000L
        const val DEFAULT_SONG_NAME = "Vybraná písnička"
    }
}
