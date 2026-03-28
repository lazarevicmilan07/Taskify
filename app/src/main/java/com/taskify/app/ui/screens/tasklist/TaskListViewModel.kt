package com.taskify.app.ui.screens.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.DeleteTaskUseCase
import com.taskify.app.domain.usecase.task.GetTasksUseCase
import com.taskify.app.domain.usecase.task.SearchTasksUseCase
import com.taskify.app.domain.usecase.task.ToggleTaskCompletionUseCase
import com.taskify.app.util.AppPreferences
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

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val searchTasksUseCase: SearchTasksUseCase,
    private val alarmScheduler: TaskAlarmScheduler,
    private val repository: TaskRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(TaskFilter.ALL)
    // Initialised from the saved default. Settings changes update this immediately
    // via the init collector; manual in-list changes are session-only (not persisted).
    private val _sortOrder = MutableStateFlow(
        runCatching { SortOrder.valueOf(appPreferences.lastSortOrder) }
            .getOrDefault(SortOrder.CREATED_DATE_DESC)
    )
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    private var recentlyDeletedTask: Task? = null

    init {
        // When the user changes the default sort in Settings, propagate it to the
        // task list immediately — but only Settings writes to appPreferences, so
        // manual in-list sort changes never bleed into the Settings default.
        viewModelScope.launch {
            appPreferences.sortOrderFlow
                .map { name -> runCatching { SortOrder.valueOf(name) }.getOrDefault(SortOrder.CREATED_DATE_DESC) }
                .collect { _sortOrder.value = it }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredTasksFlow: Flow<List<Task>> = combine(
        _activeFilter,
        _sortOrder,
        _isSearchActive
    ) { filter, sort, isSearchActive ->
        Triple(filter, sort, isSearchActive)
    }.flatMapLatest { (filter, sort, isSearchActive) ->
        if (isSearchActive) {
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
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TaskListUiState> = combine(
        filteredTasksFlow,
        _searchQuery,
        _snackbarMessage,
        _sortOrder
    ) { tasks, query, message, sort ->
        TaskListUiState(
            tasks = tasks,
            isLoading = false,
            activeFilter = _activeFilter.value,
            sortOrder = sort,
            searchQuery = query,
            isSearchActive = _isSearchActive.value,
            snackbarMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState()
    )

    fun setFilter(filter: TaskFilter) { _activeFilter.value = filter }

    // Session-only: updates the local StateFlow but does NOT persist to prefs,
    // so the Settings default sort is never affected by manual in-list sorting.
    fun setSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch { toggleCompletionUseCase(taskId, isCompleted) }
    }

    fun toggleSubTaskCompletion(taskId: String, subTaskId: String, isCompleted: Boolean) {
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

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            recentlyDeletedTask = task
            task.dueDate?.let { alarmScheduler.cancelTaskReminder(task.id, task.title) }
            deleteTaskUseCase(task.id)
            _snackbarMessage.value = "Task deleted"
        }
    }

    fun undoDelete() {
        viewModelScope.launch { _snackbarMessage.value = null }
    }

    fun dismissSnackbar() { _snackbarMessage.value = null }
}
