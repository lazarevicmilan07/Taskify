package com.taskify.app.data.local.dao

import androidx.room.*
import com.taskify.app.data.local.entity.SubTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubTaskDao {

    @Upsert
    suspend fun upsertSubTask(subTask: SubTaskEntity)

    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :subTaskId")
    suspend fun deleteSubTaskById(subTaskId: String)

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY position ASC")
    fun observeSubTasksForTask(taskId: String): Flow<List<SubTaskEntity>>

    @Query("UPDATE subtasks SET is_completed = :isCompleted WHERE id = :subTaskId")
    suspend fun updateCompletionStatus(subTaskId: String, isCompleted: Boolean)

    /** Reorder subtasks by updating their position values */
    @Query("UPDATE subtasks SET position = :position WHERE id = :subTaskId")
    suspend fun updatePosition(subTaskId: String, position: Int)
}
