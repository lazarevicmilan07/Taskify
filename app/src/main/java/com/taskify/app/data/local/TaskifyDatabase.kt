package com.taskify.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taskify.app.data.local.converter.Converters
import com.taskify.app.data.local.dao.SubTaskDao
import com.taskify.app.data.local.dao.TagDao
import com.taskify.app.data.local.dao.TaskDao
import com.taskify.app.data.local.entity.*

/**
 * Single Room database for the app.
 *
 * Version strategy: bump version + add a Migration object whenever the schema changes.
 * Avoid destructive migrations in production — they wipe user data.
 *
 * exportSchema = true: generates schema JSON files in /schemas/ for CI migration testing.
 */
@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskifyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "taskify.db"
    }
}
