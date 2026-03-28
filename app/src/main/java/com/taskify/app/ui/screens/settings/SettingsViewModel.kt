package com.taskify.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.taskify.app.util.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: AppPreferences
) : ViewModel()
