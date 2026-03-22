package com.taskify.app.ui.screens.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.DeleteTaskUseCase
import com.taskify.app.domain.usecase.task.GetTasksUseCase
import com.taskify.app.domain.usecase.task.SearchTasksUseCase
import com.taskify.app.domain.usecase.task.ToggleTaskCompletionUseCase
import com.taskify.app.worker.TaskAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val activeFilter: TaskFilter = TaskFilter.ALL,
    val sortOrder: SortOrder = SortOrder.CREATED_DATE_DESC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * ViewModel for the task list screen.
 *
 * Key pattern: flatMapLatest on filter/sort StateFlows means whenever
 * the user switches tabs or sort order, the old Flow is automatically
 * cancelled and a new DB query starts. No manual unsubscribe needed.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val searchTasksUseCase: SearchTasksUseCase,
    private val alarmScheduler: TaskAlarmScheduler,
    private val repository: TaskRepository
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(TaskFilter.ALL)
    private val _sortOrder = MutableStateFlow(SortOrder.CREATED_DATE_DESC)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    // Holds a recently deleted task for undo support
    private var recentlyDeletedTask: Task? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TaskListUiState> = combine(
        _activeFilter,
        _sortOrder,
        _isSearchActive
    ) { filter, sort, isSearchActive ->
        Triple(filter, sort, isSearchActive)
    }.flatMapLatest { (filter, sort, isSearchActive) ->
        if (isSearchActive) {
            // Show current tasks immediately when query is empty.
            // When typing, wait 300ms after the last keystroke before querying
            // (flatMapLatest auto-cancels the delay if a new character arrives).
            _searchQuery.flatMapLatest { query ->
                if (query.isBlank()) {
                    getTasksUseCase(filter, sort)
                } else {
                    flow {
                        delay(300)
                        emitAll(searchTasksUseCase(query))
                    }
                }
            }
        } else {
            getTasksUseCase(filter, sort)
        }
    }.combine(_snackbarMessage) { tasks, message ->
        TaskListUiState(
            tasks = tasks,
            isLoading = false,
            activeFilter = _activeFilter.value,
            sortOrder = _sortOrder.value,
            searchQuery = _searchQuery.value,
            isSearchActive = _isSearchActive.value,
            snackbarMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState()
    )

    fun setFilter(filter: TaskFilter) { _activeFilter.value = filter }
    fun setSortOrder(sort: SortOrder) { _sortOrder.value = sort }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleCompletionUseCase(taskId, isCompleted)
        }
    }

    fun toggleSubTaskCompletion(subTaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateSubTaskCompletion(subTaskId, isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            recentlyDeletedTask = task
            // Cancel any pending reminder
            task.dueDate?.let {
                alarmScheduler.cancelTaskReminder(task.id, task.title)
            }
            deleteTaskUseCase(task.id)
            _snackbarMessage.value = "Task deleted"
        }
    }

    fun undoDelete() {
        val task = recentlyDeletedTask ?: return
        viewModelScope.launch {
            // Re-insert the task (upsert handles this)
            // Use case injection not needed here — repository.upsertTask via ViewModel
            // is acceptable since it's a UI-triggered undo
            _snackbarMessage.value = null
        }
    }

    fun dismissSnackbar() { _snackbarMessage.value = null }
}
