package com.taskify.app.ui.screens.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskify.app.domain.model.SubTask
import com.taskify.app.domain.model.Task
import com.taskify.app.domain.model.TaskRepository
import com.taskify.app.domain.usecase.task.DeleteTaskUseCase
import com.taskify.app.domain.usecase.task.ToggleTaskCompletionUseCase
import com.taskify.app.worker.TaskAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    // Subtasks staged locally — not yet written to DB
    val pendingSubTasks: List<SubTask> = emptyList()
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val toggleCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val alarmScheduler: TaskAlarmScheduler
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private val _pendingSubTasks = MutableStateFlow<List<SubTask>>(emptyList())

    val uiState: StateFlow<TaskDetailUiState> = combine(
        repository.observeTask(taskId),
        _pendingSubTasks
    ) { task, pending ->
        TaskDetailUiState(
            task = task,
            isLoading = false,
            isDeleted = task == null,
            pendingSubTasks = pending
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskDetailUiState()
    )

    fun toggleCompletion() {
        val task = uiState.value.task ?: return
        viewModelScope.launch { toggleCompletionUseCase(taskId, !task.isCompleted) }
    }

    fun deleteTask() {
        viewModelScope.launch {
            val task = uiState.value.task ?: return@launch
            task.dueDate?.let { alarmScheduler.cancelTaskReminder(task.id, task.title) }
            deleteTaskUseCase(taskId)
        }
    }

    // ── Subtask staging ──────────────────────────────────────────────────────

    /** Adds a subtask to the local draft list — NOT saved to DB yet. */
    fun stageSubTask(title: String) {
        if (title.isBlank()) return
        val draft = SubTask(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            title = title.trim(),
            createdAt = LocalDateTime.now(),
            position = (uiState.value.task?.subtasks?.size ?: 0) + _pendingSubTasks.value.size
        )
        _pendingSubTasks.update { it + draft }
    }

    /** Removes an unsaved draft subtask. */
    fun removePendingSubTask(id: String) {
        _pendingSubTasks.update { it.filter { s -> s.id != id } }
    }

    /** Commits all pending subtasks to the DB. */
    fun savePendingSubTasks() {
        val pending = _pendingSubTasks.value
        if (pending.isEmpty()) return
        viewModelScope.launch {
            pending.forEach { repository.upsertSubTask(it) }
            _pendingSubTasks.value = emptyList()
        }
    }

    // ── Existing subtask mutations (immediate) ───────────────────────────────

    fun toggleSubTask(subTaskId: String, isCompleted: Boolean) {
        viewModelScope.launch { repository.updateSubTaskCompletion(subTaskId, isCompleted) }
    }

    fun deleteSubTask(subTaskId: String) {
        viewModelScope.launch { repository.deleteSubTask(subTaskId) }
    }
}
