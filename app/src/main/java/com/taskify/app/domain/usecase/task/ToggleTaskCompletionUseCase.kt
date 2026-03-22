package com.taskify.app.domain.usecase.task

import com.taskify.app.domain.model.TaskRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, isCompleted: Boolean) {
        repository.updateCompletionStatus(taskId, isCompleted)
        // When completing a task, cascade to all subtasks
        if (isCompleted) {
            repository.observeTask(taskId).firstOrNull()?.subtasks?.forEach { sub ->
                if (!sub.isCompleted) repository.updateSubTaskCompletion(sub.id, true)
            }
        }
    }
}
