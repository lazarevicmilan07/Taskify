package com.taskify.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.taskify.app.MainActivity
import com.taskify.app.R

/**
 * Centralizes notification channel creation and posting.
 * Channel creation is idempotent — safe to call on every app launch.
 */
object NotificationHelper {

    const val CHANNEL_TASKS = "channel_tasks"
    const val CHANNEL_REMINDERS = "channel_reminders"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"

    fun createNotificationChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // High-importance channel for due-date reminders
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming task due dates"
                enableVibration(true)
            }
        )

        // Default channel for general task notifications
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASKS,
                "Task Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General task status notifications"
            }
        )
    }

    /** Posts the task reminder notification. Called from TaskAlarmReceiver. */
    fun postTaskReminderNotification(
        context: Context,
        taskId: String,
        taskTitle: String,
        notificationId: Int
    ) {
        // Deep-link tap into the task detail screen
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action — reschedules 10 minutes from now
        val snoozeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done action — marks task complete from notification
        val doneIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(taskTitle)
            .setContentText("This task is due now")
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_snooze, "Snooze 10m", snoozePendingIntent)
            .addAction(R.drawable.ic_check, "Done", donePendingIntent)
            .build()

        // Permission check handled at runtime before scheduling (API 33+)
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
