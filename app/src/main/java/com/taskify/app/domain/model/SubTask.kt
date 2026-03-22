package com.taskify.app.domain.model

import java.time.LocalDateTime

data class SubTask(
    val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val createdAt: LocalDateTime
)
