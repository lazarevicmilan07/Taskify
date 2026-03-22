package com.taskify.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TaskifyPrimary,
    onPrimary = TaskifyOnPrimary,
    primaryContainer = TaskifyPrimaryContainer,
    onPrimaryContainer = TaskifyOnPrimaryContainer,
    secondary = TaskifySecondary,
    tertiary = TaskifyTertiary,
    background = TaskifyBackground,
    onBackground = TaskifyOnBackground,
    surface = TaskifySurface,
    onSurface = TaskifyOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = TaskifyPrimaryDark,
    onPrimary = TaskifyOnPrimaryDark,
    background = TaskifyBackgroundDark,
    surface = TaskifySurfaceDark,
    onSurface = TaskifyOnSurfaceDark
)

@Composable
fun TaskifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You dynamic color (Android 12+)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Make status bar transparent and use edge-to-edge
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TaskifyTypography,
        content = content
    )
}
