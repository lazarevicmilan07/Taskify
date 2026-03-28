package com.taskify.app.ui.screens.tasklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskify.app.domain.model.SortOrder
import com.taskify.app.domain.model.TaskFilter
import com.taskify.app.ui.components.EmptyState
import com.taskify.app.ui.components.TaskCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddTask: (initialTitle: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TaskListTopBar(
                uiState = uiState,
                scrollBehavior = scrollBehavior,
                onSearchToggle = { viewModel.setSearchActive(!uiState.isSearchActive) },
                onNavigateToSettings = onNavigateToSettings,
                onSortSelected = viewModel::setSortOrder
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddTask("") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Search bar — slides in below app bar when active
            AnimatedVisibility(
                visible = uiState.isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchInputBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClose = { viewModel.setSearchActive(false) }
                )
            }

            // Filter pills — hidden while searching
            AnimatedVisibility(
                visible = !uiState.isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                FilterTabRow(
                    activeFilter = uiState.activeFilter,
                    onFilterSelected = viewModel::setFilter
                )
            }

            // Task list
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            } else if (uiState.tasks.isEmpty()) {
                EmptyStateForFilter(filter = uiState.activeFilter, isSearching = uiState.isSearchActive)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.tasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = {
                                viewModel.toggleTaskCompletion(task.id, !task.isCompleted)
                            },
                            onDelete = { viewModel.deleteTask(task) },
                            onClick = { onNavigateToDetail(task.id) },
                            onToggleSubTask = { subTaskId, isCompleted ->
                                viewModel.toggleSubTaskCompletion(task.id, subTaskId, isCompleted)
                            },
                            modifier = Modifier.animateItem()
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListTopBar(
    uiState: TaskListUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchToggle: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSortSelected: (SortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Taskify", style = MaterialTheme.typography.titleLarge) },
        actions = {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (uiState.isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (uiState.isSearchActive) "Close search" else "Search"
                )
            }
            // Sort button — DropdownMenu is inside this Box so it anchors correctly
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Sort")
                }
                SortDropdownMenu(
                    expanded = showSortMenu,
                    currentSort = uiState.sortOrder,
                    onSortSelected = {
                        onSortSelected(it)
                        showSortMenu = false
                    },
                    onDismiss = { showSortMenu = false }
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortGroup(
            header = "Due Date",
            options = listOf(SortOrder.DUE_DATE_ASC, SortOrder.DUE_DATE_DESC),
            currentSort = currentSort,
            onSortSelected = onSortSelected
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SortGroup(
            header = "Priority",
            options = listOf(SortOrder.PRIORITY_DESC, SortOrder.PRIORITY_ASC),
            currentSort = currentSort,
            onSortSelected = onSortSelected
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SortGroup(
            header = "Date Created",
            options = listOf(SortOrder.CREATED_DATE_DESC, SortOrder.CREATED_DATE_ASC),
            currentSort = currentSort,
            onSortSelected = onSortSelected
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SortGroup(
            header = "Title",
            options = listOf(SortOrder.TITLE_ASC, SortOrder.TITLE_DESC),
            currentSort = currentSort,
            onSortSelected = onSortSelected
        )
    }
}

@Composable
private fun SortGroup(
    header: String,
    options: List<SortOrder>,
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit
) {
    Text(
        text = header,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
    options.forEach { sort ->
        DropdownMenuItem(
            text = { Text(sort.label, style = MaterialTheme.typography.bodyMedium) },
            onClick = { onSortSelected(sort) },
            trailingIcon = {
                if (currentSort == sort) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                }
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        )
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("Search tasks...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() }
            )
        )
        TextButton(onClick = onClose) {
            Text("Cancel")
        }
    }
}

private val TaskFilter.icon: ImageVector
    get() = when (this) {
        TaskFilter.ALL           -> Icons.AutoMirrored.Filled.List
        TaskFilter.TODAY         -> Icons.Filled.Today
        TaskFilter.UPCOMING      -> Icons.Filled.CalendarMonth
        TaskFilter.HIGH_PRIORITY -> Icons.Filled.Flag
        TaskFilter.COMPLETED     -> Icons.Outlined.CheckCircle
    }

// Two-line label for "High Priority" so the full text fits in the pill
private val TaskFilter.pillLabel: String
    get() = when (this) {
        TaskFilter.ALL           -> "All"
        TaskFilter.TODAY         -> "Today"
        TaskFilter.UPCOMING      -> "Upcoming"
        TaskFilter.HIGH_PRIORITY -> "High\nPriority"
        TaskFilter.COMPLETED     -> "Done"
    }

@Composable
private fun FilterTabRow(
    activeFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    // IntrinsicSize.Max makes all pills match the tallest one (the 2-line "High Priority" pill)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterPill(
                filter = filter,
                selected = activeFilter == filter,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun FilterPill(
    filter: TaskFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (selected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 7.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = filter.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = filter.pillLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyStateForFilter(filter: TaskFilter, isSearching: Boolean) {
    val (title, subtitle) = when {
        isSearching -> "No results" to "Try a different search term"
        filter == TaskFilter.TODAY -> "Nothing due today" to "Enjoy your free time!"
        filter == TaskFilter.COMPLETED -> "No completed tasks" to "Complete tasks to see them here"
        filter == TaskFilter.HIGH_PRIORITY -> "No high priority tasks" to "Mark tasks as high priority to see them here"
        else -> "No tasks yet" to "Tap + to add your first task"
    }
    EmptyState(title = title, subtitle = subtitle, modifier = Modifier.fillMaxSize())
}
