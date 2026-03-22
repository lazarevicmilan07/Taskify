package com.taskify.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single task.
 *
 * Design notes:
 * - UUID string PK: avoids int-collision when merging with remote data in future sync.
 * - dueDate stored as epoch millis (Long?) — nullable means "no due date".
 * - Indices on isCompleted + priority enable fast filtered queries without full scans.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["is_completed"]),
        Index(value = ["priority"]),
        Index(value = ["due_date"]),
        Index(value = ["created_at"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    /** 0=LOW, 1=MEDIUM, 2=HIGH — stored as int for efficient sorting */
    @ColumnInfo(name = "priority")
    val priority: Int = 1,

    /** Nullable — null means task has no due date / reminder */
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    // --- Recurring task fields ---
    /** RecurrenceType: NONE, DAILY, WEEKLY, MONTHLY, CUSTOM */
    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String = "NONE",

    /** Interval in days for CUSTOM recurrence */
    @ColumnInfo(name = "recurrence_interval")
    val recurrenceInterval: Int = 0,

    // --- Notification state ---
    /** True if a notification has been scheduled for this task */
    @ColumnInfo(name = "reminder_scheduled")
    val reminderScheduled: Boolean = false
)
