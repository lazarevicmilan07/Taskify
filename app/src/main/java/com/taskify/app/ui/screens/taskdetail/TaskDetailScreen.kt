package com.taskify.app.ui.screens.taskdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskify.app.domain.model.Priority
import com.taskify.app.domain.model.SubTask
import com.taskify.app.ui.components.TagChip
import com.taskify.app.ui.components.parseColor
import com.taskify.app.ui.theme.PriorityHigh
import com.taskify.app.ui.theme.PriorityLow
import com.taskify.app.ui.theme.PriorityMedium
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newSubTaskText by remember { mutableStateOf("") }

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
    val priorityColor = when (task.priority) {
        Priority.LOW -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH -> PriorityHigh
    }
    val hasPendingChanges = uiState.pendingSubTasks.isNotEmpty()

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
                    IconButton(onClick = { onNavigateToEdit(taskId) }) {
                        Icon(Icons.Filled.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Title row ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left priority bar
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else priorityColor)
                    )
                    // Completion toggle
                    IconButton(
                        onClick = { viewModel.toggleCompletion() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle
                                          else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = "Toggle completion",
                            tint = if (task.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                   else priorityColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (task.isCompleted) 0.45f else 1f
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Meta chips row ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority chip
                    MetaChip(
                        icon = { Icon(Icons.Filled.Flag, null, modifier = Modifier.size(12.dp)) },
                        label = task.priority.label,
                        color = priorityColor
                    )
                    // Due date chip
                    task.dueDate?.let { due ->
                        val isOverdue = due.toLocalDate().isBefore(LocalDate.now()) && !task.isCompleted
                        MetaChip(
                            icon = { Icon(Icons.Filled.DateRange, null, modifier = Modifier.size(12.dp)) },
                            label = due.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            color = if (isOverdue) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                    // Tags
                    task.tags.forEach { tag ->
                        TagChip(name = tag.name, color = parseColor(tag.color))
                    }
                }
            }

            // ── Description ────────────────────────────────────────────────
            if (task.description.isNotBlank()) {
                item {
                    Text(
                        text = task.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Divider ────────────────────────────────────────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // ── Subtasks header ────────────────────────────────────────────
            item {
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
                    val saved = task.subtasks.size
                    val done = task.subtasks.count { it.isCompleted }
                    if (saved > 0) {
                        Text(
                            "$done/$saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Saved subtask progress bar
            if (task.subtasks.isNotEmpty()) {
                item {
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
            }

            // ── Saved subtasks ─────────────────────────────────────────────
            items(task.subtasks, key = { it.id }) { subTask ->
                SubTaskRow(
                    subTask = subTask,
                    isPending = false,
                    onToggle = { viewModel.toggleSubTask(subTask.id, it) },
                    onRemove = { viewModel.deleteSubTask(subTask.id) }
                )
            }

            // ── Pending (unsaved) subtasks ─────────────────────────────────
            items(uiState.pendingSubTasks, key = { "pending_${it.id}" }) { subTask ->
                SubTaskRow(
                    subTask = subTask,
                    isPending = true,
                    onToggle = null,
                    onRemove = { viewModel.removePendingSubTask(subTask.id) }
                )
            }

            // ── Add subtask input ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubTaskText,
                        onValueChange = { newSubTaskText = it },
                        placeholder = {
                            Text("New subtask...",
                                style = MaterialTheme.typography.bodyMedium)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.stageSubTask(newSubTaskText)
                            newSubTaskText = ""
                        },
                        enabled = newSubTaskText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, "Stage subtask", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Save button — only shown when there are pending subtasks ───
            item {
                AnimatedVisibility(visible = hasPendingChanges) {
                    Button(
                        onClick = { viewModel.savePendingSubTasks() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save ${uiState.pendingSubTasks.size} subtask${if (uiState.pendingSubTasks.size > 1) "s" else ""}")
                    }
                }
            }
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
        // Indent line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    if (isPending) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else if (subTask.isCompleted) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.outlineVariant
                )
        )

        if (onToggle != null) {
            Checkbox(
                checked = subTask.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.size(20.dp)
            )
        } else {
            // Pending — not yet togglable
            Icon(
                Icons.Filled.Schedule,
                null,
                modifier = Modifier.size(20.dp).padding(2.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        Text(
            text = subTask.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun MetaChip(
    icon: @Composable () -> Unit,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides color) { icon() }
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = color)
        }
    }
}
