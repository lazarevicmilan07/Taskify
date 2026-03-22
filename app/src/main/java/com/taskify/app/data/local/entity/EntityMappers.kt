package com.taskify.app.data.local.entity

import com.taskify.app.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// ── Long (epoch millis) ↔ LocalDateTime ─────────────────────────────────────

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun LocalDateTime.toEpochMilli(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

// ── Entity → Domain ──────────────────────────────────────────────────────────

fun TaskWithDetails.toDomain(): Task = Task(
    id = task.id,
    title = task.title,
    description = task.description,
    priority = Priority.fromLevel(task.priority),
    dueDate = task.dueDate?.toLocalDateTime(),
    isCompleted = task.isCompleted,
    createdAt = task.createdAt.toLocalDateTime(),
    updatedAt = task.updatedAt.toLocalDateTime(),
    subtasks = subtasks.map { it.toDomain() },
    tags = tags.map { it.toDomain() },
    recurrence = task.recurrenceType.toRecurrence(task.recurrenceInterval),
    reminderScheduled = task.reminderScheduled
)

fun SubTaskEntity.toDomain(): SubTask = SubTask(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt.toLocalDateTime()
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, color = color)

// ── Domain → Entity ──────────────────────────────────────────────────────────

fun Task.toEntity(): TaskEntity {
    val (type, interval) = recurrence.toStorageValues()
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        priority = priority.level,
        dueDate = dueDate?.toEpochMilli(),
        isCompleted = isCompleted,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        recurrenceType = type,
        recurrenceInterval = interval,
        reminderScheduled = reminderScheduled
    )
}

fun SubTask.toEntity(): SubTaskEntity = SubTaskEntity(
    id = id,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt.toEpochMilli()
)

fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, color = color)

// ── Recurrence serialization ─────────────────────────────────────────────────

private fun String.toRecurrence(interval: Int): Recurrence = when (this) {
    "DAILY" -> Recurrence.Daily
    "WEEKLY" -> Recurrence.Weekly
    "MONTHLY" -> Recurrence.Monthly
    "CUSTOM" -> Recurrence.Custom(interval)
    else -> Recurrence.None
}

private fun Recurrence.toStorageValues(): Pair<String, Int> = when (this) {
    is Recurrence.None -> "NONE" to 0
    is Recurrence.Daily -> "DAILY" to 0
    is Recurrence.Weekly -> "WEEKLY" to 0
    is Recurrence.Monthly -> "MONTHLY" to 0
    is Recurrence.Custom -> "CUSTOM" to intervalDays
}
