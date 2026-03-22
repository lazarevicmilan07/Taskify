package com.taskify.app.domain.usecase.task

import com.taskify.app.domain.model.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) = repository.deleteTask(taskId)
}
