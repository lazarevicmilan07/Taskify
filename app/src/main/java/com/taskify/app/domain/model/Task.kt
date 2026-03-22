package com.taskify.app.domain.model

import java.time.LocalDateTime

/** Pure domain model — no Android or Room dependencies. */
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val subtasks: List<SubTask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val recurrence: Recurrence = Recurrence.None,
    val reminderScheduled: Boolean = false
) {
    /** Derived: subtask completion progress, 0.0–1.0 */
    val subtaskProgress: Float
        get() = if (subtasks.isEmpty()) 0f
                else subtasks.count { it.isCompleted }.toFloat() / subtasks.size
}

enum class Priority(val level: Int, val label: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High");

    companion object {
        fun fromLevel(level: Int): Priority = entries.find { it.level == level } ?: MEDIUM
    }
}

sealed class Recurrence {
    data object None : Recurrence()
    data object Daily : Recurrence()
    data object Weekly : Recurrence()
    data object Monthly : Recurrence()
    data class Custom(val intervalDays: Int) : Recurrence()
}
