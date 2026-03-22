package com.taskify.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Compound return type for loading a task with its subtasks and tags in one query.
 *
 * Room's @Relation annotation generates the JOIN automatically. The @Transaction
 * annotation on the DAO query guarantees all three reads happen atomically —
 * preventing a race where subtasks/tags might be modified mid-load.
 */
data class TaskWithDetails(
    @Embedded val task: TaskEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    val subtasks: List<SubTaskEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "task_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
