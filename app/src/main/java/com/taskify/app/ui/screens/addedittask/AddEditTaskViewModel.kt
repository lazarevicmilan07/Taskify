package com.taskify.app.ui.screens.addedittask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.UpsertTaskUseCase
import com.taskify.app.worker.TaskAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class AddEditUiState(
    val taskId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: LocalDateTime? = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0),
    val recurrence: Recurrence = Recurrence.None,
    val tags: List<Tag> = emptyList(),
    val pendingSubTasks: List<SubTask> = emptyList(),
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val titleError: String? = null,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val upsertTaskUseCase: UpsertTaskUseCase,
    private val alarmScheduler: TaskAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        val taskId = savedStateHandle.get<String>("taskId")
        val initialTitle = savedStateHandle.get<String>("initialTitle") ?: ""
        if (taskId != null) {
            loadExistingTask(taskId)
        } else if (initialTitle.isNotBlank()) {
            _uiState.update { it.copy(title = initialTitle) }
        }
    }

    private fun loadExistingTask(taskId: String) {
        viewModelScope.launch {
            repository.observeTask(taskId).firstOrNull()?.let { task ->
                _uiState.update { state ->
                    state.copy(
                        taskId = task.id,
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        dueDate = task.dueDate,
                        recurrence = task.recurrence,
                        tags = task.tags,
                        isEditMode = true
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }

    fun onDescriptionChange(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun onPriorityChange(priority: Priority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onDueDateChange(date: LocalDateTime?) {
        _uiState.update { it.copy(dueDate = date) }
    }

    fun onRecurrenceChange(recurrence: Recurrence) {
        _uiState.update { it.copy(recurrence = recurrence) }
    }

    fun stageSubTask(title: String) {
        if (title.isBlank()) return
        val draft = SubTask(
            id = UUID.randomUUID().toString(),
            taskId = _uiState.value.taskId,
            title = title.trim(),
            createdAt = LocalDateTime.now(),
            position = _uiState.value.pendingSubTasks.size
        )
        _uiState.update { it.copy(pendingSubTasks = it.pendingSubTasks + draft) }
    }

    fun removePendingSubTask(id: String) {
        _uiState.update { it.copy(pendingSubTasks = it.pendingSubTasks.filter { s -> s.id != id }) }
    }

    fun saveTask() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val now = LocalDateTime.now()
            val task = Task(
                id = state.taskId,
                title = state.title.trim(),
                description = state.description.trim(),
                priority = state.priority,
                dueDate = state.dueDate,
                createdAt = if (state.isEditMode) now else now, // preserve original in full impl
                updatedAt = now,
                tags = state.tags,
                recurrence = state.recurrence
            )

            val result = upsertTaskUseCase(task)
            if (result.isSuccess) {
                // Save any subtasks that were staged during creation/editing.
                // Task must exist in DB before subtasks due to FK constraint.
                state.pendingSubTasks.forEach { repository.upsertSubTask(it) }

                state.dueDate?.let { dueDate ->
                    alarmScheduler.scheduleTaskReminder(task.id, task.title, dueDate)
                }
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        titleError = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }
}
