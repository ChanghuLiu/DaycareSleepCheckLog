package com.daycare.sleepcheck.log.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.daycare.sleepcheck.log.domain.ReminderAccuracy
import com.daycare.sleepcheck.log.domain.ReminderScheduling

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val preferences = ReminderPreferences(context)

    val enabled: Boolean get() = preferences.enabled

    fun setEnabled(value: Boolean) {
        preferences.enabled = value
        if (!value) cancelAllKnownAlarms()
    }

    fun preciseAvailable(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun accuracy(): ReminderAccuracy = ReminderScheduling.accuracy(Build.VERSION.SDK_INT, preciseAvailable())

    fun notificationsAllowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun schedule(sessionId: String, roomName: String, scheduledAt: Long) {
        if (!enabled) return
        cancel(sessionId)
        val pendingIntent = pendingIntent(sessionId, roomName, scheduledAt)
        val triggerAt = scheduledAt.coerceAtLeast(System.currentTimeMillis() + MINIMUM_TRIGGER_DELAY_MS)
        if (accuracy() == ReminderAccuracy.PRECISE) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Permission can change between the capability check and scheduling.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancel(sessionId: String) {
        alarmManager.cancel(pendingIntent(sessionId, "", 0L))
    }

    suspend fun rescheduleAll(db: AppDatabase) {
        if (!enabled) return
        db.sleepDao().activeSessionsOnce().forEach { session ->
            val count = db.sleepDao().recordsForSession(session.id).size
            val roomName = db.peopleDao().room(session.roomId)?.name ?: session.roomId
            val nextAt = ReminderScheduling.nextReminderAt(session.startedAt, session.intervalMinutes, count)
            schedule(session.id, roomName, nextAt)
        }
    }

    suspend fun rescheduleSession(db: AppDatabase, sessionId: String) {
        if (!enabled) return
        val session = db.sleepDao().session(sessionId) ?: error("Sleep session not found while scheduling reminder")
        check(session.active) { "Sleep session is no longer active while scheduling reminder" }
        val count = db.sleepDao().recordsForSession(session.id).size
        val roomName = db.peopleDao().room(session.roomId)?.name ?: session.roomId
        val nextAt = ReminderScheduling.nextReminderAt(session.startedAt, session.intervalMinutes, count)
        schedule(session.id, roomName, nextAt)
    }

    private fun cancelAllKnownAlarms() {
        // AlarmManager does not expose a queryable alarm list. Each active session is
        // cancelled by SleepViewModel when a session ends; disabling reminders also
        // prevents any fired receiver from posting a notification.
    }

    private fun pendingIntent(sessionId: String, roomName: String, scheduledAt: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(ReminderAlarmReceiver.EXTRA_ROOM_NAME, roomName)
            putExtra(ReminderAlarmReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val MINIMUM_TRIGGER_DELAY_MS = 1_000L
    }
}
