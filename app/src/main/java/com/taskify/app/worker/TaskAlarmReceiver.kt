package com.taskify.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskify.app.domain.model.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives exact alarms for task reminders.
 * Also handles snooze and done actions from notification action buttons.
 *
 * @AndroidEntryPoint enables Hilt injection into a BroadcastReceiver.
 * The goAsync() pattern is critical here: BroadcastReceiver.onReceive() has
 * a 10-second deadline on the main thread; goAsync() extends this for coroutines.
 */
@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "com.taskify.app.ACTION_TASK_REMINDER"
        const val ACTION_SNOOZE = "com.taskify.app.ACTION_SNOOZE"
        const val ACTION_COMPLETE = "com.taskify.app.ACTION_COMPLETE"
        const val SNOOZE_MINUTES = 10L
    }

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var alarmScheduler: TaskAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
        val notificationId = taskId.hashCode()

        when (intent.action) {
            ACTION_FIRE -> {
                NotificationHelper.postTaskReminderNotification(
                    context, taskId, taskTitle, notificationId
                )
            }

            ACTION_SNOOZE -> {
                // Cancel current notification and reschedule 10 minutes later
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        alarmScheduler.snoozeTask(taskId, taskTitle, SNOOZE_MINUTES)
                    } finally {
                        pending.finish()
                    }
                }
                // Immediately dismiss the notification
                android.app.NotificationManager::class.java
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                nm.cancel(notificationId)
            }

            ACTION_COMPLETE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.updateCompletionStatus(taskId, true)
                        repository.updateReminderScheduled(taskId, false)
                    } finally {
                        pending.finish()
                    }
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                nm.cancel(notificationId)
            }
        }
    }
}
