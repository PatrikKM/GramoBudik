package cz.patrik.gramobudik

import android.content.Context
import android.content.SharedPreferences

class AlarmPreferences(context: Context) {

    private val prefs: SharedPreferences =
        (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var songUri: String?
        get() = prefs.getString(KEY_SONG_URI, null)
        set(value) {
            prefs.edit().putString(KEY_SONG_URI, value).apply()
        }

    var songName: String?
        get() = prefs.getString(KEY_SONG_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_SONG_NAME, value).apply()
        }

    var hour: Int
        get() = prefs.getInt(KEY_HOUR, DEFAULT_HOUR)
        set(value) {
            prefs.edit().putInt(KEY_HOUR, value).apply()
        }

    var minute: Int
        get() = prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE)
        set(value) {
            prefs.edit().putInt(KEY_MINUTE, value).apply()
        }

    var isScheduled: Boolean
        get() = prefs.getBoolean(KEY_IS_SCHEDULED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_SCHEDULED, value).apply()
        }

    var scheduledTriggerMillis: Long
        get() = prefs.getLong(KEY_SCHEDULED_TRIGGER_MILLIS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_SCHEDULED_TRIGGER_MILLIS, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "gramobudik_prefs"
        const val KEY_SONG_URI = "song_uri"
        const val KEY_SONG_NAME = "song_name"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_IS_SCHEDULED = "is_scheduled"
        const val KEY_SCHEDULED_TRIGGER_MILLIS = "scheduled_trigger_millis"
        const val DEFAULT_HOUR = 7
        const val DEFAULT_MINUTE = 0
    }
}
