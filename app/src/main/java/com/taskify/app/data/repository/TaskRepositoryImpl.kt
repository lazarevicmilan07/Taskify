package com.taskify.app.data.repository

import com.taskify.app.data.local.dao.SubTaskDao
import com.taskify.app.data.local.dao.TagDao
import com.taskify.app.data.local.dao.TaskDao
import com.taskify.app.data.local.entity.*
import com.taskify.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val subTaskDao: SubTaskDao,
    private val tagDao: TagDao
) : TaskRepository {

    // ── Observations ──────────────────────────────────────────────────────────

    override fun observeAllTasks(): Flow<List<Task>> =
        taskDao.observeAllTasksWithDetails().map { list -> list.map { it.toDomain() } }

    override fun observeActiveTasks(): Flow<List<Task>> =
        taskDao.observeActiveTasks().map { list -> list.map { it.toDomain() } }

    override fun observeCompletedTasks(): Flow<List<Task>> =
        taskDao.observeCompletedTasks().map { list -> list.map { it.toDomain() } }

    override fun observeTodayTasks(): Flow<List<Task>> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val start = today.atStartOfDay().toEpochMilli()
        val end = today.atTime(LocalTime.MAX).toEpochMilli()
        return taskDao.observeTodayTasks(start, end).map { list -> list.map { it.toDomain() } }
    }

    override fun observeUpcomingTasks(): Flow<List<Task>> {
        val startOfTomorrow = LocalDate.now(ZoneId.systemDefault())
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return taskDao.observeUpcomingTasks(startOfTomorrow).map { list -> list.map { it.toDomain() } }
    }

    override fun observeHighPriorityTasks(): Flow<List<Task>> =
        taskDao.observeHighPriorityTasks().map { list -> list.map { it.toDomain() } }

    override fun observeTask(taskId: String): Flow<Task?> =
        taskDao.observeTaskWithDetails(taskId).map { it?.toDomain() }

    override fun searchTasks(query: String): Flow<List<Task>> =
        taskDao.searchTasks(query).map { list -> list.map { it.toDomain() } }

    override fun observeTasksSorted(sortOrder: SortOrder): Flow<List<Task>> = when (sortOrder) {
        SortOrder.DUE_DATE_ASC, SortOrder.DUE_DATE_DESC -> taskDao.observeActiveTasksSortedByDueDate()
        SortOrder.PRIORITY_DESC, SortOrder.PRIORITY_ASC -> taskDao.observeActiveTasksSortedByPriority()
        else -> taskDao.observeActiveTasksSortedByCreatedDate()
    }.map { list -> list.map { it.toDomain() } }

    // ── Task mutations ────────────────────────────────────────────────────────

    override suspend fun upsertTask(task: Task) {
        taskDao.upsertTask(task.toEntity())
        // Replace all tags for the task atomically
        tagDao.removeAllTagsFromTask(task.id)
        task.tags.forEach { tag ->
            tagDao.addTagToTask(TaskTagCrossRef(task.id, tag.id))
        }
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTaskById(taskId)
        // Subtasks + cross-refs cascade-deleted by FK constraints
    }

    override suspend fun updateCompletionStatus(taskId: String, isCompleted: Boolean) {
        taskDao.updateCompletionStatus(
            taskId = taskId,
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
    }

    // ── Subtask mutations ─────────────────────────────────────────────────────

    override suspend fun upsertSubTask(subTask: SubTask) {
        subTaskDao.upsertSubTask(subTask.toEntity())
    }

    override suspend fun deleteSubTask(subTaskId: String) {
        subTaskDao.deleteSubTaskById(subTaskId)
    }

    override suspend fun updateSubTaskCompletion(subTaskId: String, isCompleted: Boolean) {
        subTaskDao.updateCompletionStatus(subTaskId, isCompleted)
    }

    // ── Tag mutations ─────────────────────────────────────────────────────────

    override fun observeAllTags(): Flow<List<Tag>> =
        tagDao.observeAllTags().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertTag(tag: Tag) {
        tagDao.upsertTag(tag.toEntity())
    }

    override suspend fun deleteTag(tagId: String) {
        val entity = tagDao.getTagById(tagId) ?: return
        tagDao.deleteTag(entity)
    }

    override suspend fun addTagToTask(taskId: String, tagId: String) {
        tagDao.addTagToTask(TaskTagCrossRef(taskId, tagId))
    }

    override suspend fun removeTagFromTask(taskId: String, tagId: String) {
        tagDao.removeTagFromTask(TaskTagCrossRef(taskId, tagId))
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    override suspend fun getTasksWithPendingReminders(): List<Task> =
        taskDao.getTasksWithPendingReminders(System.currentTimeMillis()).map { entity ->
            // Lightweight — no subtasks/tags needed for rescheduling
            TaskWithDetails(entity).toDomain()
        }

    override suspend fun updateReminderScheduled(taskId: String, scheduled: Boolean) {
        taskDao.updateReminderScheduled(taskId, scheduled)
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun LocalDateTime.toEpochMilli(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
