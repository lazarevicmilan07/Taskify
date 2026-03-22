package com.taskify.app.data.local.dao

import androidx.room.*
import com.taskify.app.data.local.entity.TaskEntity
import com.taskify.app.data.local.entity.TaskWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * DAO for task CRUD and filtered queries.
 *
 * All list-returning functions emit Flow so the UI automatically reacts to DB changes
 * without polling. @Transaction on multi-relation queries prevents partial reads.
 */
@Dao
interface TaskDao {

    // ── Writes ──────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("UPDATE tasks SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateCompletionStatus(taskId: String, isCompleted: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET reminder_scheduled = :scheduled WHERE id = :taskId")
    suspend fun updateReminderScheduled(taskId: String, scheduled: Boolean)

    // ── Single task reads ────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskWithDetails(taskId: String): Flow<TaskWithDetails?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    // ── List queries — all return Flow for reactivity ─────────────────────

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun observeAllTasksWithDetails(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY priority DESC, due_date ASC")
    fun observeActiveTasks(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE is_completed = 1 ORDER BY updated_at DESC")
    fun observeCompletedTasks(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE is_completed = 0
          AND due_date >= :startOfDay
          AND due_date < :endOfDay
        ORDER BY due_date ASC
    """)
    fun observeTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE is_completed = 0
          AND due_date >= :from
        ORDER BY due_date ASC
    """)
    fun observeUpcomingTasks(from: Long): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE is_completed = 0
          AND priority = 2
        ORDER BY due_date ASC
    """)
    fun observeHighPriorityTasks(): Flow<List<TaskWithDetails>>

    // ── Search — FTS-style LIKE query (use FTS5 table for large datasets) ──

    @Transaction
    @Query("""
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskWithDetails>>

    // ── Sorted variants ──────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_date ASC")
    fun observeActiveTasksSortedByDueDate(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY priority DESC")
    fun observeActiveTasksSortedByPriority(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY created_at DESC")
    fun observeActiveTasksSortedByCreatedDate(): Flow<List<TaskWithDetails>>

    // ── Notification helpers ─────────────────────────────────────────────────

    /** Called by BootReceiver to reschedule all pending reminders after reboot */
    @Query("""
        SELECT * FROM tasks
        WHERE reminder_scheduled = 1
          AND is_completed = 0
          AND due_date > :now
    """)
    suspend fun getTasksWithPendingReminders(now: Long): List<TaskEntity>
}
