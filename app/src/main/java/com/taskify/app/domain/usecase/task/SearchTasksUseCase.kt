package com.taskify.app.domain.usecase.task

import com.taskify.app.domain.model.Task
import com.taskify.app.domain.model.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SearchTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    /** Returns empty list immediately for blank queries to avoid unnecessary DB hits. */
    operator fun invoke(query: String): Flow<List<Task>> {
        if (query.isBlank()) return flowOf(emptyList())
        return repository.searchTasks(query.trim())
    }
}
