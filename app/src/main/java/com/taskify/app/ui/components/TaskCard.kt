package com.taskify.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskify.app.domain.model.Priority
import com.taskify.app.domain.model.Task
import com.taskify.app.ui.theme.PriorityHigh
import com.taskify.app.ui.theme.PriorityLow
import com.taskify.app.ui.theme.PriorityMedium
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onToggleSubTask: (subTaskId: String, isCompleted: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = { SwipeBackground(dismissState) },
        content = {
            TaskCardContent(
                task = task,
                onToggleComplete = onToggleComplete,
                onClick = onClick,
                onToggleSubTask = onToggleSubTask
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState) {
    val direction = state.dismissDirection
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }
    val scale by animateFloatAsState(
        targetValue = if (state.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
        label = "swipe_icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
            Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Icon(
            imageVector = if (direction == SwipeToDismissBoxValue.StartToEnd)
                Icons.Outlined.CheckCircle else Icons.Filled.Delete,
            contentDescription = null,
            modifier = Modifier.scale(scale),
            tint = if (direction == SwipeToDismissBoxValue.StartToEnd)
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun TaskCardContent(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onToggleSubTask: (subTaskId: String, isCompleted: Boolean) -> Unit
) {
    val priorityColor = when (task.priority) {
        Priority.LOW -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH -> PriorityHigh
    }
    val textAlpha = if (task.isCompleted) 0.4f else 1f
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            // ── Main task row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left priority bar
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant
                            else priorityColor
                        )
                )

                Spacer(Modifier.width(12.dp))

                // Completion circle
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleComplete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle
                                      else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                               else priorityColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title + meta
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val hasMeta = task.dueDate != null || task.tags.isNotEmpty() || task.subtasks.isNotEmpty()
                    if (hasMeta) {
                        Spacer(Modifier.height(3.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            task.dueDate?.let { due ->
                                Text(
                                    text = formatDueDate(due),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = dueDateColor(due, task.isCompleted)
                                )
                            }
                            task.tags.take(2).forEach { tag ->
                                TagChip(name = tag.name, color = parseColor(tag.color))
                            }
                            if (task.subtasks.isNotEmpty()) {
                                val done = task.subtasks.count { it.isCompleted }
                                Text(
                                    text = "$done/${task.subtasks.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Chevron expand button — only when subtasks exist
                if (task.subtasks.isNotEmpty()) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand subtasks",
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(chevronRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── Expanded subtask rows ────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 27.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    task.subtasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubTask(sub.id, !sub.isCompleted) }
                                .padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (sub.isCompleted) Icons.Outlined.CheckCircle
                                              else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (sub.isCompleted)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (sub.isCompleted) 0.4f else 0.85f
                                ),
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shows "Today", "Tomorrow", "Yesterday", or the actual short date. */
private fun formatDueDate(dateTime: LocalDateTime): String {
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

@Composable
private fun dueDateColor(dueDate: LocalDateTime, isCompleted: Boolean): Color {
    if (isCompleted) return MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val today = LocalDate.now()
    val date = dueDate.toLocalDate()
    return when {
        date.isBefore(today) -> MaterialTheme.colorScheme.error
        date == today -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun PriorityIndicator(priority: Priority, modifier: Modifier = Modifier) {
    val color = when (priority) {
        Priority.LOW -> PriorityLow
        Priority.MEDIUM -> PriorityMedium
        Priority.HIGH -> PriorityHigh
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun TagChip(name: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(3.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}

/** Parses "#AARRGGBB" or "#RRGGBB" hex color strings. */
fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF6200EE)
}
