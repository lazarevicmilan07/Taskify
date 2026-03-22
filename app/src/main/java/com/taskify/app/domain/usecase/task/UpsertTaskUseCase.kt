package com.taskify.app.domain.usecase.task

import com.taskify.app.domain.model.Task
import com.taskify.app.domain.model.TaskRepository
import javax.inject.Inject

/** Validates and saves a task. Returns a Result to surface validation errors to the UI. */
class UpsertTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    sealed class Error {
        data object TitleEmpty : Error()
        data object TitleTooLong : Error()
    }

    suspend operator fun invoke(task: Task): Result<Unit> {
        if (task.title.isBlank()) return Result.failure(Exception("Title cannot be empty"))
        if (task.title.length > 200) return Result.failure(Exception("Title too long"))
        repository.upsertTask(task)
        return Result.success(Unit)
    }
}
