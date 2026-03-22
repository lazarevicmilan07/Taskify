package com.taskify.app.domain

import com.taskify.app.domain.model.*
import com.taskify.app.domain.usecase.task.UpsertTaskUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class UpsertTaskUseCaseTest {

    private lateinit var repository: TaskRepository
    private lateinit var useCase: UpsertTaskUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true) // relaxed = mock all calls with default values
        useCase = UpsertTaskUseCase(repository)
    }

    private fun validTask(title: String = "Valid Title") = Task(
        id = "test-id",
        title = title,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Test
    fun `valid task is saved successfully`() = runTest {
        val result = useCase(validTask())
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.upsertTask(any()) }
    }

    @Test
    fun `blank title returns failure`() = runTest {
        val result = useCase(validTask(title = "  "))
        assertTrue(result.isFailure)
        coVerify(exactly = 0) { repository.upsertTask(any()) }
    }

    @Test
    fun `title over 200 chars returns failure`() = runTest {
        val longTitle = "A".repeat(201)
        val result = useCase(validTask(title = longTitle))
        assertTrue(result.isFailure)
    }

    @Test
    fun `title exactly 200 chars is valid`() = runTest {
        val result = useCase(validTask(title = "A".repeat(200)))
        assertTrue(result.isSuccess)
    }
}
