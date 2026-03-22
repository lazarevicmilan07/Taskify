package com.taskify.app.domain.model

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defined in the domain layer.
 *
 * The domain layer depends on this abstraction — the actual Room implementation
 * lives in the data layer and is injected by Hilt. This inversion of control
 * makes use cases testable without a real database.
 */
interface TaskRepository {

    fun observeAllTasks(): Flow<List<Task>>
    fun observeActiveTasks(): Flow<List<Task>>
    fun observeCompletedTasks(): Flow<List<Task>>
    fun observeTodayTasks(): Flow<List<Task>>
    fun observeUpcomingTasks(): Flow<List<Task>>
    fun observeHighPriorityTasks(): Flow<List<Task>>
    fun observeTask(taskId: String): Flow<Task?>
    fun searchTasks(query: String): Flow<List<Task>>
    fun observeTasksSorted(sortOrder: SortOrder): Flow<List<Task>>

    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun updateCompletionStatus(taskId: String, isCompleted: Boolean)

    // Subtasks
    suspend fun upsertSubTask(subTask: SubTask)
    suspend fun deleteSubTask(subTaskId: String)
    suspend fun updateSubTaskCompletion(subTaskId: String, isCompleted: Boolean)

    // Tags
    fun observeAllTags(): Flow<List<Tag>>
    suspend fun upsertTag(tag: Tag)
    suspend fun deleteTag(tagId: String)
    suspend fun addTagToTask(taskId: String, tagId: String)
    suspend fun removeTagFromTask(taskId: String, tagId: String)

    // Notification helpers
    suspend fun getTasksWithPendingReminders(): List<Task>
    suspend fun updateReminderScheduled(taskId: String, scheduled: Boolean)
}
