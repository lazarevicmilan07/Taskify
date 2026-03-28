package com.taskify.app.domain.usecase.task

import com.taskify.app.domain.model.TaskRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, isCompleted: Boolean) {
        repository.updateCompletionStatus(taskId, isCompleted)
        // Cascade completion state to all subtasks in both directions
        val subtasks = repository.observeTask(taskId).firstOrNull()?.subtasks ?: return
        if (isCompleted) {
            subtasks.forEach { sub ->
                if (!sub.isCompleted) repository.updateSubTaskCompletion(sub.id, true)
            }
        } else {
            subtasks.forEach { sub ->
                if (sub.isCompleted) repository.updateSubTaskCompletion(sub.id, false)
            }
        }
    }
}
