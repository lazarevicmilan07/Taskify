package com.taskify.app.di

import android.content.Context
import androidx.room.Room
import com.taskify.app.data.local.TaskifyDatabase
import com.taskify.app.data.local.dao.SubTaskDao
import com.taskify.app.data.local.dao.TagDao
import com.taskify.app.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TaskifyDatabase =
        Room.databaseBuilder(context, TaskifyDatabase::class.java, TaskifyDatabase.DATABASE_NAME)
            // fallbackToDestructiveMigration only acceptable in dev; use real migrations in prod
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideTaskDao(db: TaskifyDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideSubTaskDao(db: TaskifyDatabase): SubTaskDao = db.subTaskDao()

    @Provides
    fun provideTagDao(db: TaskifyDatabase): TagDao = db.tagDao()
}
