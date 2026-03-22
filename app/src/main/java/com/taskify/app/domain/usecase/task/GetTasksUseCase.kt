package com.taskify.app.domain.usecase.task

import com.taskify.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Encapsulates the logic for fetching the correct task list given a filter + sort order.
 *
 * Single-responsibility: ViewModels call this instead of calling the repository directly,
 * keeping filter-switching logic out of the ViewModel.
 *
 * Sorting is applied in-memory via .map so it works uniformly across all filter queries.
 */
class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(
        filter: TaskFilter,
        sortOrder: SortOrder = SortOrder.CREATED_DATE_DESC
    ): Flow<List<Task>> = when (filter) {
        TaskFilter.ALL -> repository.observeAllTasks()
        TaskFilter.TODAY -> repository.observeTodayTasks()
        TaskFilter.UPCOMING -> repository.observeUpcomingTasks()
        TaskFilter.COMPLETED -> repository.observeCompletedTasks()
        TaskFilter.HIGH_PRIORITY -> repository.observeHighPriorityTasks()
    }.map { tasks ->
        when (sortOrder) {
            SortOrder.DUE_DATE_ASC    -> tasks.sortedWith(compareBy(nullsLast()) { it.dueDate })
            SortOrder.DUE_DATE_DESC   -> tasks.sortedWith(compareByDescending(nullsFirst()) { it.dueDate })
            SortOrder.PRIORITY_DESC   -> tasks.sortedByDescending { it.priority.level }
            SortOrder.PRIORITY_ASC    -> tasks.sortedBy { it.priority.level }
            SortOrder.CREATED_DATE_DESC -> tasks.sortedByDescending { it.createdAt }
            SortOrder.CREATED_DATE_ASC  -> tasks.sortedBy { it.createdAt }
            SortOrder.TITLE_ASC       -> tasks.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC      -> tasks.sortedByDescending { it.title.lowercase() }
        }
    }
}
