package com.taskify.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Lightweight SharedPreferences wrapper for user settings.
 * For a production app, consider DataStore (Proto or Preferences) for
 * coroutine-friendly, type-safe, async access.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("taskify_prefs", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    var defaultPriority: String
        get() = prefs.getString(KEY_DEFAULT_PRIORITY, "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit { putString(KEY_DEFAULT_PRIORITY, value) }

    var lastSortOrder: String
        get() = prefs.getString(KEY_SORT_ORDER, "CREATED_DATE") ?: "CREATED_DATE"
        set(value) = prefs.edit { putString(KEY_SORT_ORDER, value) }

    var themeMode: String
        get() = prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit { putString(KEY_THEME, value) }

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_DEFAULT_PRIORITY = "default_priority"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_THEME = "theme_mode"
    }
}
