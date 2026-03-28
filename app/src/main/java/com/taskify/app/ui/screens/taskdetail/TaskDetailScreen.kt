package com.taskify.app.ui.screens.taskdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskify.app.domain.model.Priority
import com.taskify.app.domain.model.Recurrence
import com.taskify.app.domain.model.SubTask
import com.taskify.app.ui.theme.PriorityHigh
import com.taskify.app.ui.theme.PriorityLow
import com.taskify.app.ui.theme.PriorityMedium
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        return
    }

    val task = uiState.task ?: return
    val priorityColor = when (uiState.priority) {
        Priority.LOW -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH -> PriorityHigh
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Save button — only active when there are unsaved changes
                    TextButton(
                        onClick = { viewModel.saveChanges() },
                        enabled = uiState.isDirty && !uiState.isSaving && uiState.title.isNotBlank()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp))
                        } else {
                            Text(
                                "Save",
                                color = if (uiState.isDirty && uiState.title.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {

            // ── Completion toggle + Title ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left priority bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant
                            else priorityColor
                        )
                )

                // Completion circle
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.toggleCompletion() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle
                                      else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = "Toggle completion",
                        tint = if (task.isCompleted)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else priorityColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Editable title
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (task.isCompleted) 0.45f else 1f
                        )
                    ),
                    placeholder = { Text("Task title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        focusedBorderColor = priorityColor.copy(alpha = 0.7f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Description ──────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Add description...") },
                minLines = 2,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))

            // ── Priority ─────────────────────────────────────────────────────
            DetailRow(label = "Priority", icon = Icons.Filled.Flag) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = uiState.priority == p,
                            onClick = { viewModel.onPriorityChange(p) },
                            label = { Text(p.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Due date ─────────────────────────────────────────────────────
            DetailRow(label = "Due Date", icon = Icons.Filled.DateRange) {
                DueDatePicker(
                    dueDate = uiState.dueDate,
                    onDateSelected = viewModel::onDueDateChange
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Recurrence ───────────────────────────────────────────────────
            DetailRow(label = "Repeat", icon = Icons.Filled.Refresh) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        "None" to Recurrence.None,
                        "Daily" to Recurrence.Daily,
                        "Weekly" to Recurrence.Weekly,
                        "Monthly" to Recurrence.Monthly
                    ).forEach { (label, rec) ->
                        FilterChip(
                            selected = uiState.recurrence == rec,
                            onClick = { viewModel.onRecurrenceChange(rec) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))

            // ── Subtasks header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Subtasks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val done = task.subtasks.count { it.isCompleted }
                val total = task.subtasks.size
                if (total > 0) {
                    Text(
                        "$done/$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            if (task.subtasks.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { task.subtaskProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            // Saved subtasks
            task.subtasks.forEach { sub ->
                SubTaskRow(
                    subTask = sub,
                    isPending = false,
                    onToggle = { viewModel.toggleSubTask(sub.id, it) },
                    onRemove = { viewModel.deleteSubTask(sub.id) }
                )
            }

            // Pending subtasks
            uiState.pendingSubTasks.forEach { sub ->
                SubTaskRow(
                    subTask = sub,
                    isPending = true,
                    onToggle = null,
                    onRemove = { viewModel.removePendingSubTask(sub.id) }
                )
            }

            // Subtask input row
            SubTaskInputRow(
                onAdd = { viewModel.stageSubTask(it) }
            )

            // Commit pending subtasks
            AnimatedVisibility(visible = uiState.pendingSubTasks.isNotEmpty()) {
                Button(
                    onClick = { viewModel.savePendingSubTasks() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    val n = uiState.pendingSubTasks.size
                    Text("Save $n subtask${if (n > 1) "s" else ""}")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete task?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTask(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePicker(
    dueDate: LocalDateTime?,
    onDateSelected: (LocalDateTime?) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.let {
            it.toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    )
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = { showPicker = true },
            label = {
                Text(
                    dueDate?.format(formatter) ?: "Set date",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.DateRange, null, modifier = Modifier.size(14.dp))
            }
        )
        if (dueDate != null) {
            IconButton(
                onClick = { onDateSelected(null) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Close, "Clear date",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        onDateSelected(LocalDateTime.of(date, LocalTime.of(9, 0)))
                    }
                    showPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun SubTaskInputRow(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("New subtask...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        FilledTonalIconButton(
            onClick = { onAdd(text); text = "" },
            enabled = text.isNotBlank()
        ) {
            Icon(Icons.Filled.Add, "Add subtask", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SubTaskRow(
    subTask: SubTask,
    isPending: Boolean,
    onToggle: ((Boolean) -> Unit)?,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        if (onToggle != null) {
            Checkbox(
                checked = subTask.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                Icons.Filled.Schedule, null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(2.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        Text(
            text = subTask.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = when {
                isPending -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                subTask.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (!isPending && subTask.isCompleted) TextDecoration.LineThrough else null
        )

        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Close, "Remove",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
