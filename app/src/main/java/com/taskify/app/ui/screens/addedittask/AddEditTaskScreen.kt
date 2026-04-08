package com.taskify.app.ui.screens.addedittask

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskify.app.domain.model.Priority
import com.taskify.app.domain.model.Recurrence
import com.taskify.app.domain.model.SubTask
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: String?,
    initialTitle: String = "",
    onNavigateBack: () -> Unit,
    viewModel: AddEditTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Task" else "New Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveTask,
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp))
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Task title *") },
                isError = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )

            // Priority selector
            PrioritySelector(
                selected = uiState.priority,
                onPrioritySelected = viewModel::onPriorityChange
            )

            // Due date
            DueDatePicker(
                dueDate = uiState.dueDate,
                onDateSelected = viewModel::onDueDateChange
            )

            // Recurrence
            RecurrenceSelector(
                selected = uiState.recurrence,
                onRecurrenceSelected = viewModel::onRecurrenceChange
            )

            // Subtasks
            SubtasksSection(
                pendingSubTasks = uiState.pendingSubTasks,
                onStageSubTask = viewModel::stageSubTask,
                onRemoveSubTask = viewModel::removePendingSubTask,
                onUpdateSubTask = viewModel::updateSubTaskTitle,
                onSubTasksReordered = viewModel::onSubTasksReordered
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtasksSection(
    pendingSubTasks: List<SubTask>,
    onStageSubTask: (String) -> Unit,
    onRemoveSubTask: (String) -> Unit,
    onUpdateSubTask: (id: String, newTitle: String) -> Unit,
    onSubTasksReordered: (List<SubTask>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    // Local list drives UI during drag; syncs from ViewModel when not dragging
    var localSubTasks by remember { mutableStateOf(pendingSubTasks) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    // Inline edit state
    var editingSubTaskId by remember { mutableStateOf<String?>(null) }

    // BringIntoView for the "add subtask" input row at the bottom
    val addFieldBringIntoView = remember { BringIntoViewRequester() }

    LaunchedEffect(pendingSubTasks) {
        if (draggedId == null) localSubTasks = pendingSubTasks
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Subtasks", style = MaterialTheme.typography.labelLarge)

        localSubTasks.forEach { sub ->
            key(sub.id) {
                val isDragging = sub.id == draggedId
                val editBringIntoView = remember { BringIntoViewRequester() }
                val editFocusRequester = remember { FocusRequester() }
                var editText by remember(sub.id) { mutableStateOf(sub.title) }
                // Guard against the initial onFocusChanged(false) that fires before
                // requestFocus() is called — only commit/dismiss on real focus loss.
                var fieldWasFocused by remember { mutableStateOf(false) }

                LaunchedEffect(sub.title) {
                    if (editingSubTaskId != sub.id) editText = sub.title
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) draggedOffset else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .onSizeChanged { size ->
                            if (itemHeightPx == 0f && size.height > 0) {
                                itemHeightPx = size.height + with(density) { 4.dp.roundToPx() }.toFloat()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingSubTaskId == sub.id) {
                        // ── Inline edit mode ──
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier
                                .weight(1f)
                                .bringIntoViewRequester(editBringIntoView)
                                .focusRequester(editFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        fieldWasFocused = true
                                        scope.launch {
                                            delay(300)
                                            editBringIntoView.bringIntoView()
                                        }
                                    } else if (fieldWasFocused) {
                                        // Real focus loss (not the initial pre-focus false fire)
                                        fieldWasFocused = false
                                        if (editText.isNotBlank() && editText != sub.title) {
                                            onUpdateSubTask(sub.id, editText)
                                        }
                                        editingSubTaskId = null
                                    }
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (editText.isNotBlank()) onUpdateSubTask(sub.id, editText)
                                    editingSubTaskId = null
                                    fieldWasFocused = false
                                    focusManager.clearFocus()
                                }
                            )
                        )

                        LaunchedEffect(Unit) {
                            fieldWasFocused = false
                            editFocusRequester.requestFocus()
                        }
                    } else {
                        // ── Read mode — tap to edit ──
                        Text(
                            text = sub.title,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editText = sub.title
                                    fieldWasFocused = false
                                    editingSubTaskId = sub.id
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = { onRemoveSubTask(sub.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close, "Remove",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Drag handle — RIGHT side, PointerEventPass.Initial so we win over the
                    // parent verticalScroll even when the form is tall enough to scroll.
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = "Drag to reorder",
                        modifier = Modifier
                            .size(20.dp)
                            .pointerInput(sub.id) {
                                awaitEachGesture {
                                    // Consume the down event in Initial pass — this prevents the
                                    // parent verticalScroll from ever seeing it and stealing the gesture.
                                    val down = awaitPointerEvent(PointerEventPass.Initial)
                                    val downChange = down.changes
                                        .firstOrNull { it.pressed && !it.previousPressed }
                                        ?: return@awaitEachGesture
                                    downChange.consume()

                                    draggedId = sub.id
                                    draggedOffset = 0f
                                    val pointerId = downChange.id

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            val change = event.changes
                                                .firstOrNull { it.id == pointerId }
                                                ?: break
                                            change.consume()

                                            if (!change.pressed) {
                                                onSubTasksReordered(localSubTasks)
                                                break
                                            }

                                            draggedOffset += change.position.y - change.previousPosition.y

                                            val fromIdx = localSubTasks.indexOfFirst { it.id == sub.id }
                                            if (fromIdx != -1 && itemHeightPx > 0f) {
                                                val center = fromIdx * itemHeightPx + itemHeightPx / 2f + draggedOffset
                                                val toIdx = (center / itemHeightPx).toInt()
                                                    .coerceIn(0, localSubTasks.lastIndex)
                                                if (toIdx != fromIdx) {
                                                    val list = localSubTasks.toMutableList()
                                                    list.add(toIdx, list.removeAt(fromIdx))
                                                    localSubTasks = list
                                                    draggedOffset += (fromIdx - toIdx) * itemHeightPx
                                                }
                                            }
                                        }
                                    } finally {
                                        draggedId = null
                                        draggedOffset = 0f
                                    }
                                }
                            },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // ── Add subtask input row ──
        var addText by remember { mutableStateOf("") }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(addFieldBringIntoView),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = addText,
                onValueChange = { addText = it },
                placeholder = { Text("Add a subtask...") },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                addFieldBringIntoView.bringIntoView()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (addText.isNotBlank()) {
                            onStageSubTask(addText)
                            addText = ""
                            scope.launch {
                                delay(100)
                                addFieldBringIntoView.bringIntoView()
                            }
                        }
                    }
                )
            )
            FilledTonalIconButton(
                onClick = {
                    if (addText.isNotBlank()) {
                        onStageSubTask(addText)
                        addText = ""
                        scope.launch {
                            delay(100)
                            addFieldBringIntoView.bringIntoView()
                        }
                    }
                },
                enabled = addText.isNotBlank()
            ) {
                Icon(Icons.Filled.Add, "Add subtask", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PrioritySelector(
    selected: Priority,
    onPrioritySelected: (Priority) -> Unit
) {
    Column {
        Text("Priority", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { priority ->
                FilterChip(
                    selected = selected == priority,
                    onClick = { onPrioritySelected(priority) },
                    label = { Text(priority.label) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePicker(
    dueDate: LocalDateTime?,
    onDateSelected: (LocalDateTime?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.let {
            it.toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    )

    OutlinedCard(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Due Date", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = dueDate?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
                        ?: "No due date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.DateRange, contentDescription = null)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(LocalDateTime.of(date, LocalTime.of(9, 0)))
                    }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun RecurrenceSelector(
    selected: Recurrence,
    onRecurrenceSelected: (Recurrence) -> Unit
) {
    Column {
        Text("Recurrence", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            val options = listOf(
                "None" to Recurrence.None,
                "Daily" to Recurrence.Daily,
                "Weekly" to Recurrence.Weekly,
                "Monthly" to Recurrence.Monthly
            )
            options.forEach { (label, recurrence) ->
                FilterChip(
                    selected = selected == recurrence,
                    onClick = { onRecurrenceSelected(recurrence) },
                    label = { Text(label) }
                )
            }
        }
    }
}
