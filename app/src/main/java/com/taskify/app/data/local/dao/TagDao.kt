package com.taskify.app.data.local.dao

import androidx.room.*
import com.taskify.app.data.local.entity.TagEntity
import com.taskify.app.data.local.entity.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Upsert
    suspend fun upsertTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: String): TagEntity?

    // ── Cross-ref management ─────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToTask(crossRef: TaskTagCrossRef)

    @Delete
    suspend fun removeTagFromTask(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id = :taskId")
    suspend fun removeAllTagsFromTask(taskId: String)

    @Query("SELECT tag_id FROM task_tag_cross_ref WHERE task_id = :taskId")
    suspend fun getTagIdsForTask(taskId: String): List<String>
}
