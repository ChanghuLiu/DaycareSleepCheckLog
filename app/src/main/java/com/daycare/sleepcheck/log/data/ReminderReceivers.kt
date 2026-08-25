package com.daycare.sleepcheck.log.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daycare.sleepcheck.log.MainActivity
import com.daycare.sleepcheck.log.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!ReminderPreferences(context).enabled) return@launch
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) return@launch
                createChannel(context)
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
                val roomName = intent.getStringExtra(EXTRA_ROOM_NAME).orEmpty()
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_OPEN_SESSION_ID, sessionId)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val openPendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    sessionId.hashCode(),
                    openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(context.getString(R.string.notification_body, roomName))
                    .setContentIntent(openPendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                NotificationManagerCompat.from(context).notify(sessionId.hashCode(), notification)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_ROOM_NAME = "extra_room_name"
        const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"
        const val CHANNEL_ID = "sleep_check_reminders"

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                )
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            }
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val database = AppDatabase.create(context)
            try { ReminderScheduler(context).rescheduleAll(database) } finally { database.close(); pendingResult.finish() }
        }
    }
}
