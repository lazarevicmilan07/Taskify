package com.taskify.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskify.app.domain.model.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/**
 * Reschedules all pending task reminders after device reboot.
 *
 * AlarmManager alarms are wiped on reboot — this receiver catches
 * BOOT_COMPLETED and restores them from DB state.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var alarmScheduler: TaskAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = repository.getTasksWithPendingReminders()
                tasks.forEach { task ->
                    val dueDate = task.dueDate ?: return@forEach
                    alarmScheduler.scheduleTaskReminder(task.id, task.title, dueDate)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
