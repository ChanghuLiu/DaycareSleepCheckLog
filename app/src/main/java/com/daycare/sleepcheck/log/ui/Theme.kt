package com.daycare.sleepcheck.log.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
        primary = DaycareColors.DeepGreen,
        onPrimary = Color.White,
        primaryContainer = DaycareColors.SoftGreen,
        onPrimaryContainer = DaycareColors.Ink,
        secondary = DaycareColors.LeafGreen,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE7F2EA),
        onSecondaryContainer = DaycareColors.Ink,
        background = DaycareColors.WarmBackground,
        surface = Color.White,
        surfaceVariant = Color(0xFFE8EEE9),
        onSurface = DaycareColors.Ink,
        onSurfaceVariant = DaycareColors.MutedInk,
        outline = Color(0xFFB9C9BD),
    )
    val dark = darkColorScheme(
        primary = Color(0xFF9BD1AE),
        onPrimary = Color(0xFF063A27),
        primaryContainer = Color(0xFF26513E),
        onPrimaryContainer = Color(0xFFD0F2DE),
        secondary = Color(0xFFA7D3BE),
        onSecondary = Color(0xFF123729),
        secondaryContainer = Color(0xFF26513E),
        onSecondaryContainer = Color(0xFFD0F2DE),
        background = Color(0xFF101713),
        surface = Color(0xFF18221C),
        surfaceVariant = Color(0xFF3F4943),
        onSurface = Color(0xFFE0E4DF),
        onSurfaceVariant = Color(0xFFBFC9C1),
        outline = Color(0xFF87988C),
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                val barColor = if (darkMode) AndroidColor.rgb(16, 23, 19) else AndroidColor.rgb(248, 250, 247)
                window.statusBarColor = barColor
                window.navigationBarColor = barColor
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkMode
                controller.isAppearanceLightNavigationBars = !darkMode
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkMode) dark else light,
        typography = Typography().let { typography ->
            typography.copy(
                headlineLarge = typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                headlineMedium = typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                titleLarge = typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            )
        },
        content = content,
    )
}
