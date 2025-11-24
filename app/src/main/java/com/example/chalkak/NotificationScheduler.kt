package com.example.chalkak

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class NotificationScheduler(private val context: Context) {
    companion object {
        private const val REQUEST_CODE = 2001
        private const val INTERVAL_1_MINUTE = 60 * 1000L // Convert 1 minute to milliseconds
    }

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleRepeatingNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerTime = System.currentTimeMillis() + INTERVAL_1_MINUTE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use setExactAndAllowWhileIdle for Android 12+
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Use regular alarm if permission is not available
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    INTERVAL_1_MINUTE,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use setExactAndAllowWhileIdle for Android 6.0+
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            // Use setRepeating for earlier versions
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                INTERVAL_1_MINUTE,
                pendingIntent
            )
        }
    }

    fun cancelRepeatingNotification() {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }
}

/**
 * BroadcastReceiver that receives and displays notifications
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showStudyReminderNotification()

        // Reschedule next notification (after 1 minute)
        val scheduler = NotificationScheduler(context)
        scheduler.scheduleRepeatingNotification()
    }
}

