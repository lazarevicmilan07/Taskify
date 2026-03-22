package com.taskify.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskify.app.domain.model.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels exact alarms for task due-date reminders.
 *
 * setExactAndAllowWhileIdle() fires even during Doze mode — essential
 * for time-sensitive reminders. Requires SCHEDULE_EXACT_ALARM permission (API 31+).
 */
@Singleton
class TaskAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TaskRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules a notification for the task at its due date.
     * No-op if dueDate is null or already in the past.
     */
    suspend fun scheduleTaskReminder(taskId: String, taskTitle: String, dueDate: LocalDateTime) {
        val triggerMillis = dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(taskId, taskTitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Exact alarms not permitted — fallback to inexact (best effort)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }

        repository.updateReminderScheduled(taskId, true)
    }

    /** Cancels a previously scheduled reminder. */
    suspend fun cancelTaskReminder(taskId: String, taskTitle: String) {
        alarmManager.cancel(buildPendingIntent(taskId, taskTitle))
        repository.updateReminderScheduled(taskId, false)
    }

    /** Snoozes by rescheduling [snoozeMinutes] from now. */
    fun snoozeTask(taskId: String, taskTitle: String, snoozeMinutes: Long) {
        val triggerMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(snoozeMinutes)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            buildPendingIntent(taskId, taskTitle)
        )
    }

    private fun buildPendingIntent(taskId: String, taskTitle: String): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_FIRE
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_TASK_TITLE, taskTitle)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
