package com.taskify.app.ui

import app.cash.turbine.test
import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.DeleteTaskUseCase
import com.taskify.app.domain.usecase.task.GetTasksUseCase
import com.taskify.app.domain.usecase.task.SearchTasksUseCase
import com.taskify.app.domain.usecase.task.ToggleTaskCompletionUseCase
import com.taskify.app.ui.screens.tasklist.TaskListViewModel
import com.taskify.app.worker.TaskAlarmScheduler
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var toggleCompletionUseCase: ToggleTaskCompletionUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var searchTasksUseCase: SearchTasksUseCase
    private lateinit var alarmScheduler: TaskAlarmScheduler
    private lateinit var viewModel: TaskListViewModel

    private val sampleTasks = listOf(
        Task("1", "Task 1", createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()),
        Task("2", "Task 2", priority = Priority.HIGH, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        getTasksUseCase = mockk()
        toggleCompletionUseCase = mockk(relaxed = true)
        deleteTaskUseCase = mockk(relaxed = true)
        searchTasksUseCase = mockk()
        alarmScheduler = mockk(relaxed = true)

        every { getTasksUseCase(any(), any()) } returns flowOf(sampleTasks)
        every { searchTasksUseCase(any()) } returns flowOf(emptyList())

        viewModel = TaskListViewModel(
            getTasksUseCase,
            toggleCompletionUseCase,
            deleteTaskUseCase,
            searchTasksUseCase,
            alarmScheduler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads tasks`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.tasks.size)
            assertEquals(TaskFilter.ALL, state.activeFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter changes active filter`() = runTest {
        every { getTasksUseCase(TaskFilter.TODAY, any()) } returns flowOf(listOf(sampleTasks[0]))

        viewModel.setFilter(TaskFilter.TODAY)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TaskFilter.TODAY, state.activeFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleTaskCompletion calls use case`() = runTest {
        viewModel.toggleTaskCompletion("1", true)
        coVerify { toggleCompletionUseCase("1", true) }
    }

    @Test
    fun `deleteTask calls use case and shows snackbar`() = runTest {
        val task = sampleTasks[0]
        viewModel.deleteTask(task)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Task deleted", state.snackbarMessage)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { deleteTaskUseCase("1") }
    }
}
