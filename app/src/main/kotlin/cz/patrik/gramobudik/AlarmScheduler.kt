package cz.patrik.gramobudik

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    private const val SHOW_INTENT_REQUEST_CODE = 2000

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun computeNextTriggerMillis(
        hour: Int,
        minute: Int,
        repeatDays: Set<Int> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        if (repeatDays.isEmpty()) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return calendar.timeInMillis
        }

        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (dayOffset in 0..7) {
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in repeatDays && candidate.timeInMillis > nowMillis) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candidate.timeInMillis
    }

    fun schedule(context: Context, alarm: AlarmItem): AlarmItem {
        val appContext = context.applicationContext ?: context
        val triggerAtMillis = computeNextTriggerMillis(
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDays = alarm.repeatDays,
        )
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = buildAlarmPendingIntent(appContext, alarm.id)
        val showActivityIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showIntent = PendingIntent.getActivity(
            appContext,
            SHOW_INTENT_REQUEST_CODE,
            showActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        val updated = alarm.copy(
            isEnabled = true,
            nextTriggerMillis = triggerAtMillis,
        )
        AlarmPreferences(appContext).upsertAlarm(updated)
        return updated
    }

    fun cancel(context: Context, alarmId: Int) {
        val appContext = context.applicationContext ?: context
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(appContext, alarmId)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        val prefs = AlarmPreferences(appContext)
        prefs.getAlarm(alarmId)?.let { existing ->
            prefs.upsertAlarm(
                existing.copy(
                    isEnabled = false,
                    nextTriggerMillis = 0L,
                ),
            )
        }
    }

    fun rescheduleAllEnabled(context: Context) {
        val appContext = context.applicationContext ?: context
        if (!canScheduleExactAlarms(appContext)) {
            return
        }
        val prefs = AlarmPreferences(appContext)
        prefs.getAlarms().forEach { alarm ->
            if (alarm.isEnabled && !alarm.songUri.isNullOrBlank()) {
                try {
                    schedule(appContext, alarm)
                } catch (_: SecurityException) {
                    prefs.upsertAlarm(alarm.copy(isEnabled = false, nextTriggerMillis = 0L))
                }
            }
        }
    }

    private fun buildAlarmPendingIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            data = Uri.parse("gramobudik://alarm/$alarmId")
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
