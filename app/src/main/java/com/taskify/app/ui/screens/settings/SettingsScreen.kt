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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskify.app.ui.theme.ThemeMode
import com.taskify.app.ui.theme.ThemeViewModel
import com.taskify.app.util.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var notificationsEnabled by remember { mutableStateOf(prefs.notificationsEnabled) }
    var defaultPriority by remember { mutableStateOf(prefs.defaultPriority) }
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

            item {
                ListItem(
                    headlineContent = { Text("Default priority") },
                    supportingContent = { Text(defaultPriority) },
                    leadingContent = { Icon(Icons.Filled.Flag, null) },
                    trailingContent = {
                        TextButton(onClick = {
                            defaultPriority = when (defaultPriority) {
                                "LOW" -> "MEDIUM"
                                "MEDIUM" -> "HIGH"
                                else -> "LOW"
                            }
                            prefs.defaultPriority = defaultPriority
                        }) { Text(defaultPriority) }
                    }
                )
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
            ) {
                Text(mode.label)
            }
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

// Short label used in the segmented control buttons
private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT  -> "Light"
        ThemeMode.DARK   -> "Dark"
    }

// Full label used in the list item description
private val ThemeMode.fullLabel: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.LIGHT  -> "Light"
        ThemeMode.DARK   -> "Dark"
    }
