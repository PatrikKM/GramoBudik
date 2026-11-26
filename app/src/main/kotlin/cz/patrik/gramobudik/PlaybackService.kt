package cz.patrik.gramobudik

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class PlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlaybackAndSelf()
            }

            ACTION_START -> {
                val songUri = intent.getStringExtra(EXTRA_SONG_URI)
                val songName = intent.getStringExtra(EXTRA_SONG_NAME)
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_SONG_NAME
                val openRingingUi = intent.getBooleanExtra(EXTRA_OPEN_RINGING_UI, false)
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, false)

                startForegroundPlayback(songUri, songName, openRingingUi, vibrate)
            }

            else -> {
                if (mediaPlayer == null) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopVibration()
        releaseMediaPlayer()
        releaseWakeLock()
        PlaybackState.update(false, "")
        super.onDestroy()
    }

    private fun startForegroundPlayback(
        songUri: String?,
        songName: String,
        openRingingUi: Boolean,
        vibrate: Boolean,
    ) {
        createNotificationChannel()
        val notification = buildNotification(songName, openRingingUi)

        PlaybackState.update(true, songName)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )

        if (songUri.isNullOrBlank()) {
            stopPlaybackAndSelf()
            return
        }

        acquireWakeLock()
        forceMaxAlarmVolume()
        if (vibrate) {
            startVibration()
        } else {
            stopVibration()
        }
        startMediaPlayer(songUri)
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(songName: String, openRingingUi: Boolean): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            CONTENT_REQUEST_CODE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_vinyl)
            .setContentTitle("GramoBudík – Přehrávání")
            .setContentText("$songName • Hlasitost 100 %")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Zastavit", stopPendingIntent)

        if (openRingingUi) {
            val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                FULLSCREEN_REQUEST_CODE,
                ringingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        return builder.build()
    }

    private fun acquireWakeLock() {
        val wl = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }

        wl.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        val wl = wakeLock ?: return
        wakeLock = null
        try {
            if (wl.isHeld) {
                wl.release()
            }
        } catch (_: RuntimeException) {
        }
    }

    private fun forceMaxAlarmVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (_: SecurityException) {
        }
    }

    private fun startMediaPlayer(songUri: String) {
        releaseMediaPlayer()

        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        requestAlarmAudioFocus(alarmAttributes)

        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(alarmAttributes)
            player.setDataSource(applicationContext, Uri.parse(songUri))
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.setVolume(1.0f, 1.0f)
            player.isLooping = false
            player.setOnCompletionListener { completedPlayer ->
                if (mediaPlayer === completedPlayer) {
                    stopPlaybackAndSelf()
                }
            }
            player.setOnErrorListener { erroredPlayer, what, extra ->
                Log.e(LOG_TAG, "MediaPlayer error: what=$what extra=$extra uri=$songUri")
                if (mediaPlayer === erroredPlayer) {
                    stopPlaybackAndSelf()
                }
                true
            }
            player.setOnPreparedListener { preparedPlayer ->
                if (mediaPlayer === preparedPlayer) {
                    try {
                        forceMaxAlarmVolume()
                        preparedPlayer.start()
                        Log.i(LOG_TAG, "MediaPlayer started for uri=$songUri")
                    } catch (e: IllegalStateException) {
                        Log.e(LOG_TAG, "Failed to start MediaPlayer", e)
                        stopPlaybackAndSelf()
                    }
                }
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to prepare MediaPlayer for uri=$songUri", e)
            stopPlaybackAndSelf()
        }
    }

    private fun requestAlarmAudioFocus(alarmAttributes: AudioAttributes) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAttributes)
            .setAcceptsDelayedFocusGain(false)
            .build()
        audioFocusRequest = focusRequest
        val result = audioManager.requestAudioFocus(focusRequest)
        Log.i(LOG_TAG, "requestAudioFocus result=$result")
    }

    private fun abandonAlarmAudioFocus() {
        val request = audioFocusRequest ?: return
        audioFocusRequest = null
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocusRequest(request)
    }

    private fun releaseMediaPlayer() {
        abandonAlarmAudioFocus()
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: IllegalStateException) {
        }
        try {
            player.reset()
        } catch (_: IllegalStateException) {
        }
        try {
            player.release()
        } catch (_: Exception) {
        }
    }

    private fun startVibration() {
        val vib = vibrator ?: resolveVibrator().also { vibrator = it } ?: return
        if (!vib.hasVibrator()) return
        try {
            val effect = VibrationEffect.createWaveform(longArrayOf(0L, 600L, 400L), 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vib.vibrate(effect, attrs)
            } else {
                @Suppress("DEPRECATION")
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vib.vibrate(effect, audioAttrs)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun stopPlaybackAndSelf() {
        stopVibration()
        releaseMediaPlayer()
        releaseWakeLock()
        PlaybackState.update(false, "")
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "cz.patrik.gramobudik.action.START"
        const val ACTION_STOP = "cz.patrik.gramobudik.action.STOP"
        const val EXTRA_SONG_URI = "extra_song_uri"
        const val EXTRA_SONG_NAME = "extra_song_name"
        const val EXTRA_OPEN_RINGING_UI = "extra_open_ringing_ui"
        const val EXTRA_VIBRATE = "extra_vibrate"

        private const val CHANNEL_ID = "gramobudik_playback"
        private const val CHANNEL_NAME = "Přehrávání budíku"
        private const val CHANNEL_DESCRIPTION = "Oznámení při přehrávání vybrané písničky budíkem"
        private const val NOTIFICATION_ID = 1001
        private const val CONTENT_REQUEST_CODE = 2001
        private const val FULLSCREEN_REQUEST_CODE = 2002
        private const val STOP_REQUEST_CODE = 2003
        private const val WAKE_LOCK_TAG = "gramobudik:playback"
        private const val WAKE_LOCK_TIMEOUT_MS = 60 * 60 * 1000L
        private const val DEFAULT_SONG_NAME = "Vybraná písnička"
        private const val LOG_TAG = "GramoBudikPlayback"

        fun startPlayback(
            context: Context,
            songUri: String,
            songName: String,
            openRingingUi: Boolean = false,
            vibrate: Boolean = false,
        ) {
            val appContext = context.applicationContext ?: context
            val intent = Intent(appContext, PlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SONG_URI, songUri)
                putExtra(EXTRA_SONG_NAME, songName)
                putExtra(EXTRA_OPEN_RINGING_UI, openRingingUi)
                putExtra(EXTRA_VIBRATE, vibrate)
            }
            ContextCompat.startForegroundService(appContext, intent)
        }

        fun stopPlayback(context: Context) {
            val appContext = context.applicationContext ?: context
            val intent = Intent(appContext, PlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                appContext.startService(intent)
            } catch (_: IllegalStateException) {
                PlaybackState.update(false, "")
            } catch (_: SecurityException) {
                PlaybackState.update(false, "")
            }
        }
    }
}
