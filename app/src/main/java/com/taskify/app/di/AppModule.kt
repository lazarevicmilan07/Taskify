package com.taskify.app.di

import com.taskify.app.data.repository.TaskRepositoryImpl
import com.taskify.app.domain.model.TaskRepository
import com.taskify.app.domain.usecase.task.DefaultNaturalLanguageParser
import com.taskify.app.domain.usecase.task.NaturalLanguageParserService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds interfaces to their implementations.
 * Using @Binds instead of @Provides is more efficient — Dagger avoids
 * generating an extra wrapper class at compile time.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindNlpParser(impl: DefaultNaturalLanguageParser): NaturalLanguageParserService
}
