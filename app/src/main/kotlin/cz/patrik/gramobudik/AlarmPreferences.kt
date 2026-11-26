package cz.patrik.gramobudik

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class AlarmItem(
    val id: Int,
    val hour: Int = 7,
    val minute: Int = 0,
    val songUri: String? = null,
    val songName: String? = null,
    val isEnabled: Boolean = false,
    val vibrate: Boolean = true,
    val repeatDays: Set<Int> = emptySet(),
    val nextTriggerMillis: Long = 0L,
)

class AlarmPreferences(context: Context) {

    private val prefs: SharedPreferences =
        (context.applicationContext ?: context).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAlarms(): List<AlarmItem> {
        ensureMigrated()
        val raw = prefs.getString(KEY_ALARMS_JSON, null) ?: return emptyList()
        return parseAlarms(raw).sortedWith(compareBy({ it.hour * 60 + it.minute }, { it.id }))
    }

    fun getAlarm(id: Int): AlarmItem? {
        return getAlarms().firstOrNull { it.id == id }
    }

    fun upsertAlarm(item: AlarmItem) {
        val current = getAlarms().toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            current[idx] = item
        } else {
            current.add(item)
        }
        saveAlarms(current)
    }

    fun deleteAlarm(id: Int) {
        val current = getAlarms().filterNot { it.id == id }
        saveAlarms(current)
    }

    fun allocateAlarmId(): Int {
        ensureMigrated()
        val nextId = prefs.getInt(KEY_NEXT_ALARM_ID, INITIAL_ALARM_ID)
        prefs.edit().putInt(KEY_NEXT_ALARM_ID, nextId + 1).apply()
        return nextId
    }

    private fun saveAlarms(alarms: List<AlarmItem>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            val daysArray = JSONArray()
            alarm.repeatDays.sorted().forEach { daysArray.put(it) }
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("songUri", alarm.songUri ?: JSONObject.NULL)
                put("songName", alarm.songName ?: JSONObject.NULL)
                put("isEnabled", alarm.isEnabled)
                put("vibrate", alarm.vibrate)
                put("repeatDays", daysArray)
                put("nextTriggerMillis", alarm.nextTriggerMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ALARMS_JSON, array.toString()).apply()
    }

    private fun parseAlarms(raw: String): List<AlarmItem> {
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val daysSet = mutableSetOf<Int>()
                    val daysArray = obj.optJSONArray("repeatDays")
                    if (daysArray != null) {
                        for (j in 0 until daysArray.length()) {
                            val day = daysArray.optInt(j, -1)
                            if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                                daysSet.add(day)
                            }
                        }
                    }
                    add(
                        AlarmItem(
                            id = obj.getInt("id"),
                            hour = obj.optInt("hour", DEFAULT_HOUR),
                            minute = obj.optInt("minute", DEFAULT_MINUTE),
                            songUri = obj.optString("songUri", "").takeIf { it.isNotBlank() && it != "null" },
                            songName = obj.optString("songName", "").takeIf { it.isNotBlank() && it != "null" },
                            isEnabled = obj.optBoolean("isEnabled", false),
                            vibrate = obj.optBoolean("vibrate", true),
                            repeatDays = daysSet,
                            nextTriggerMillis = obj.optLong("nextTriggerMillis", 0L),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun ensureMigrated() {
        if (prefs.contains(KEY_ALARMS_JSON)) {
            return
        }
        val legacySongUri = prefs.getString(KEY_LEGACY_SONG_URI, null)
        val legacySongName = prefs.getString(KEY_LEGACY_SONG_NAME, null)
        val legacyHour = prefs.getInt(KEY_LEGACY_HOUR, DEFAULT_HOUR)
        val legacyMinute = prefs.getInt(KEY_LEGACY_MINUTE, DEFAULT_MINUTE)
        val legacyScheduled = prefs.getBoolean(KEY_LEGACY_IS_SCHEDULED, false)
        val legacyTrigger = prefs.getLong(KEY_LEGACY_TRIGGER_MILLIS, 0L)

        val initialItem = AlarmItem(
            id = INITIAL_ALARM_ID,
            hour = legacyHour,
            minute = legacyMinute,
            songUri = legacySongUri,
            songName = legacySongName,
            isEnabled = legacyScheduled && !legacySongUri.isNullOrBlank(),
            vibrate = true,
            repeatDays = emptySet(),
            nextTriggerMillis = if (legacyScheduled) legacyTrigger else 0L,
        )
        prefs.edit().putInt(KEY_NEXT_ALARM_ID, INITIAL_ALARM_ID + 1).apply()
        saveAlarms(listOf(initialItem))
    }

    private companion object {
        const val PREFS_NAME = "gramobudik_prefs"
        const val KEY_ALARMS_JSON = "alarms_json"
        const val KEY_NEXT_ALARM_ID = "next_alarm_id"
        const val INITIAL_ALARM_ID = 1001

        const val KEY_LEGACY_SONG_URI = "song_uri"
        const val KEY_LEGACY_SONG_NAME = "song_name"
        const val KEY_LEGACY_HOUR = "hour"
        const val KEY_LEGACY_MINUTE = "minute"
        const val KEY_LEGACY_IS_SCHEDULED = "is_scheduled"
        const val KEY_LEGACY_TRIGGER_MILLIS = "scheduled_trigger_millis"

        const val DEFAULT_HOUR = 7
        const val DEFAULT_MINUTE = 0
    }
}
