package com.taskify.app.ui.screens.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.DeleteTaskUseCase
import com.taskify.app.domain.usecase.task.ToggleTaskCompletionUseCase
import com.taskify.app.domain.usecase.task.UpsertTaskUseCase
import com.taskify.app.worker.TaskAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

private data class EditableTaskState(
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val recurrence: Recurrence = Recurrence.None
) {
    companion object {
        fun from(task: Task) = EditableTaskState(
            title = task.title,
            description = task.description,
            priority = task.priority,
            dueDate = task.dueDate,
            recurrence = task.recurrence
        )
    }
}

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val pendingSubTasks: List<SubTask> = emptyList(),
    // Editable fields — managed locally, initialised from task on first load
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val recurrence: Recurrence = Recurrence.None,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val toggleCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val upsertTaskUseCase: UpsertTaskUseCase,
    private val alarmScheduler: TaskAlarmScheduler
) : ViewModel() {

    private val taskId: String = checkNotNull(savedStateHandle["taskId"])
    private val _pendingSubTasks = MutableStateFlow<List<SubTask>>(emptyList())
    private val _editState = MutableStateFlow<EditableTaskState?>(null)
    private val _isDirty = MutableStateFlow(false)
    private val _isSaving = MutableStateFlow(false)

    init {
        // Load editable fields once — the reactive Flow below handles only subtask updates
        viewModelScope.launch {
            repository.observeTask(taskId).firstOrNull()?.let { task ->
                _editState.value = EditableTaskState.from(task)
            }
        }
    }

    val uiState: StateFlow<TaskDetailUiState> = combine(
        repository.observeTask(taskId),
        _editState,
        _pendingSubTasks,
        combine(_isDirty, _isSaving) { d, s -> d to s }
    ) { task, edit, pending, (dirty, saving) ->
        if (task == null) return@combine TaskDetailUiState(isLoading = false, isDeleted = true)
        if (edit == null) return@combine TaskDetailUiState(isLoading = true)
        TaskDetailUiState(
            task = task,
            isLoading = false,
            pendingSubTasks = pending,
            title = edit.title,
            description = edit.description,
            priority = edit.priority,
            dueDate = edit.dueDate,
            recurrence = edit.recurrence,
            isDirty = dirty,
            isSaving = saving
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskDetailUiState()
    )

    // ── Field change handlers ────────────────────────────────────────────────

    fun onTitleChange(title: String) {
        _editState.update { it?.copy(title = title) }
        _isDirty.value = true
    }

    fun onDescriptionChange(desc: String) {
        _editState.update { it?.copy(description = desc) }
        _isDirty.value = true
    }

    fun onPriorityChange(priority: Priority) {
        _editState.update { it?.copy(priority = priority) }
        _isDirty.value = true
    }

    fun onDueDateChange(date: LocalDateTime?) {
        _editState.update { it?.copy(dueDate = date) }
        _isDirty.value = true
    }

    fun onRecurrenceChange(recurrence: Recurrence) {
        _editState.update { it?.copy(recurrence = recurrence) }
        _isDirty.value = true
    }

    // ── Save / delete / toggle ───────────────────────────────────────────────

    fun saveChanges() {
        val edit = _editState.value ?: return
        if (edit.title.isBlank()) return
        val task = uiState.value.task ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val updated = task.copy(
                title = edit.title.trim(),
                description = edit.description.trim(),
                priority = edit.priority,
                dueDate = edit.dueDate,
                recurrence = edit.recurrence,
                updatedAt = LocalDateTime.now()
            )
            upsertTaskUseCase(updated)
            if (edit.dueDate != null) {
                alarmScheduler.scheduleTaskReminder(task.id, edit.title.trim(), edit.dueDate)
            } else {
                alarmScheduler.cancelTaskReminder(task.id, task.title)
            }
            _isDirty.value = false
            _isSaving.value = false
        }
    }

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

    fun removePendingSubTask(id: String) {
        _pendingSubTasks.update { it.filter { s -> s.id != id } }
    }

    fun savePendingSubTasks() {
        val pending = _pendingSubTasks.value
        if (pending.isEmpty()) return
        viewModelScope.launch {
            pending.forEach { repository.upsertSubTask(it) }
            _pendingSubTasks.value = emptyList()
        }
    }

    fun toggleSubTask(subTaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateSubTaskCompletion(subTaskId, isCompleted)
            val task = repository.observeTask(taskId).firstOrNull() ?: return@launch
            if (isCompleted) {
                if (!task.isCompleted && task.subtasks.isNotEmpty() && task.subtasks.all { it.isCompleted }) {
                    repository.updateCompletionStatus(taskId, true)
                }
            } else {
                if (task.isCompleted) repository.updateCompletionStatus(taskId, false)
            }
        }
    }

    fun deleteSubTask(subTaskId: String) {
        viewModelScope.launch { repository.deleteSubTask(subTaskId) }
    }

    fun updateSubTaskTitle(id: String, newTitle: String) {
        if (newTitle.isBlank()) return
        val sub = uiState.value.task?.subtasks?.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.upsertSubTask(sub.copy(title = newTitle.trim()))
        }
    }

    fun onSubTasksReordered(subtasks: List<SubTask>) {
        viewModelScope.launch {
            subtasks.forEachIndexed { index, sub ->
                repository.upsertSubTask(sub.copy(position = index))
            }
        }
    }
}
