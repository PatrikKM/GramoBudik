package cz.patrik.gramobudik

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    private const val ALARM_REQUEST_CODE = 1001
    private const val SHOW_INTENT_REQUEST_CODE = 1002

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
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
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

    fun schedule(context: Context, hour: Int, minute: Int): Long {
        val appContext = context.applicationContext ?: context
        val triggerAtMillis = computeNextTriggerMillis(hour, minute)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pendingIntent = buildAlarmPendingIntent(appContext)
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

        AlarmPreferences(appContext).apply {
            this.hour = hour
            this.minute = minute
            this.isScheduled = true
            this.scheduledTriggerMillis = triggerAtMillis
        }

        return triggerAtMillis
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext ?: context
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(appContext)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        AlarmPreferences(appContext).apply {
            isScheduled = false
            scheduledTriggerMillis = 0L
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
