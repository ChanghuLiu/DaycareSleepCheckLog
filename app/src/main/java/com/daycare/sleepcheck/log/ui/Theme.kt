package com.daycare.sleepcheck.log.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

@Composable
fun SleepCheckTheme(content: @Composable () -> Unit) {
    val darkMode = isSystemInDarkTheme()
    val light = lightColorScheme(
        primary = Color(0xFF356859),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB9DCC9),
        onPrimaryContainer = Color(0xFF0B3324),
        secondary = Color(0xFF5C8D78),
        background = Color(0xFFF7FBF8),
        surface = Color(0xFFF7FBF8),
    )
    val dark = darkColorScheme(
        primary = Color(0xFF9FD2B4),
        onPrimary = Color(0xFF063A27),
        primaryContainer = Color(0xFF26513E),
        onPrimaryContainer = Color(0xFFD0F2DE),
        secondary = Color(0xFFA7D3BE),
        onSecondary = Color(0xFF123729),
        background = Color(0xFF121815),
        surface = Color(0xFF121815),
        surfaceVariant = Color(0xFF3F4943),
        onSurface = Color(0xFFE0E4DF),
        onSurfaceVariant = Color(0xFFBFC9C1),
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val barColor = if (darkMode) AndroidColor.rgb(18, 24, 21) else AndroidColor.rgb(247, 251, 248)
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkMode
                controller.isAppearanceLightNavigationBars = !darkMode
            }
        }
    }
    MaterialTheme(colorScheme = if (darkMode) dark else light, content = content)
}
