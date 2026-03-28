package com.taskify.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskify.app.domain.model.SortOrder
import com.taskify.app.ui.theme.ThemeMode
import com.taskify.app.ui.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs = settingsViewModel.prefs
    var notificationsEnabled by remember { mutableStateOf(prefs.notificationsEnabled) }
    var defaultPriority by remember { mutableStateOf(prefs.defaultPriority) }
    var defaultSort by remember {
        mutableStateOf(
            runCatching { SortOrder.valueOf(prefs.lastSortOrder) }
                .getOrDefault(SortOrder.CREATED_DATE_DESC)
        )
    }
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {

            // ── Appearance ─────────────────────────────────────────────────
            item { SettingsSectionHeader("Appearance") }

            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = { Text(themeMode.fullLabel) },
                    leadingContent = {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.LIGHT  -> Icons.Filled.LightMode
                                ThemeMode.DARK   -> Icons.Filled.DarkMode
                                ThemeMode.SYSTEM -> Icons.Filled.Contrast
                            },
                            contentDescription = null
                        )
                    }
                )
            }

            item {
                ThemeSegmentedControl(
                    selected = themeMode,
                    onSelect = { themeViewModel.setTheme(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
            }

            item { HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

            // ── Notifications ──────────────────────────────────────────────
            item { SettingsSectionHeader("Notifications") }

            item {
                ListItem(
                    headlineContent = { Text("Task reminders") },
                    supportingContent = { Text("Notify when tasks are due") },
                    leadingContent = { Icon(Icons.Filled.Notifications, null) },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                prefs.notificationsEnabled = it
                            }
                        )
                    }
                )
            }

            item { HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

            // ── Tasks ──────────────────────────────────────────────────────
            item { SettingsSectionHeader("Tasks") }

            // Default priority — segmented control (Low / Medium / High)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Flag, null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Default priority", style = MaterialTheme.typography.bodyMedium)
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("LOW" to "Low", "MEDIUM" to "Medium", "HIGH" to "High")
                            .forEachIndexed { index, (value, label) ->
                                SegmentedButton(
                                    selected = defaultPriority == value,
                                    onClick = {
                                        defaultPriority = value
                                        prefs.defaultPriority = value
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, 3)
                                ) { Text(label) }
                            }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // Default sort order — 4 rows of 2-option segmented controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.SwapVert, null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Default sort order", style = MaterialTheme.typography.bodyMedium)
                    }

                    SortOrderPicker(
                        selected = defaultSort,
                        onSelect = {
                            defaultSort = it
                            prefs.lastSortOrder = it.name
                        }
                    )
                }
            }

            item { HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent = { Icon(Icons.Filled.Info, null) }
                )
            }
        }
    }
}

// ── Sort order picker ─────────────────────────────────────────────────────────
// Four groups, each with 2 segmented buttons. Selecting any option immediately
// updates the in-memory state, the prefs, and (via sortOrderFlow) the task list.

@Composable
private fun SortOrderPicker(selected: SortOrder, onSelect: (SortOrder) -> Unit) {
    val groups = listOf(
        "Due Date"     to listOf(SortOrder.DUE_DATE_ASC     to "Earliest", SortOrder.DUE_DATE_DESC     to "Latest"),
        "Priority"     to listOf(SortOrder.PRIORITY_DESC    to "High → Low", SortOrder.PRIORITY_ASC    to "Low → High"),
        "Date Created" to listOf(SortOrder.CREATED_DATE_DESC to "Newest",   SortOrder.CREATED_DATE_ASC  to "Oldest"),
        "Title"        to listOf(SortOrder.TITLE_ASC         to "A → Z",    SortOrder.TITLE_DESC         to "Z → A")
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        groups.forEach { (groupLabel, options) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    groupLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(72.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    options.forEachIndexed { index, (sort, label) ->
                        SegmentedButton(
                            selected = selected == sort,
                            onClick = { onSelect(sort) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Shared composables / extensions ──────────────────────────────────────────

@Composable
private fun ThemeSegmentedControl(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                icon = {
                    Icon(
                        imageVector = when (mode) {
                            ThemeMode.LIGHT  -> Icons.Filled.LightMode
                            ThemeMode.DARK   -> Icons.Filled.DarkMode
                            ThemeMode.SYSTEM -> Icons.Filled.Contrast
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) { Text(mode.label) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT  -> "Light"
        ThemeMode.DARK   -> "Dark"
    }

private val ThemeMode.fullLabel: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.LIGHT  -> "Light"
        ThemeMode.DARK   -> "Dark"
    }
