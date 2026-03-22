package com.taskify.app.domain

import app.cash.turbine.test
import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.GetTasksUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class GetTasksUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var useCase: GetTasksUseCase

    private val sampleTask = Task(
        id = "1",
        title = "Sample Task",
        priority = Priority.HIGH,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetTasksUseCase(repository)
    }

    @Test
    fun `ALL filter with CREATED_DATE returns active tasks`() = runTest {
        every { repository.observeActiveTasks() } returns flowOf(listOf(sampleTask))

        useCase(TaskFilter.ALL, SortOrder.CREATED_DATE).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(sampleTask.title, result[0].title)
            awaitComplete()
        }
    }

    @Test
    fun `TODAY filter calls observeTodayTasks`() = runTest {
        every { repository.observeTodayTasks() } returns flowOf(listOf(sampleTask))

        useCase(TaskFilter.TODAY).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test
    fun `COMPLETED filter calls observeCompletedTasks`() = runTest {
        val completed = sampleTask.copy(isCompleted = true)
        every { repository.observeCompletedTasks() } returns flowOf(listOf(completed))

        useCase(TaskFilter.COMPLETED).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assert(result[0].isCompleted)
            awaitComplete()
        }
    }

    @Test
    fun `HIGH_PRIORITY filter calls observeHighPriorityTasks`() = runTest {
        every { repository.observeHighPriorityTasks() } returns flowOf(listOf(sampleTask))

        useCase(TaskFilter.HIGH_PRIORITY).test {
            assertEquals(Priority.HIGH, awaitItem()[0].priority)
            awaitComplete()
        }
    }

    @Test
    fun `ALL filter with DUE_DATE sort calls observeTasksSorted`() = runTest {
        every { repository.observeTasksSorted(SortOrder.DUE_DATE) } returns flowOf(listOf(sampleTask))

        useCase(TaskFilter.ALL, SortOrder.DUE_DATE).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }
}
