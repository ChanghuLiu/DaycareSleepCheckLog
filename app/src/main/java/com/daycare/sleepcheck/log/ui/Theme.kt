package com.daycare.sleepcheck.log.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SleepCheckTheme(content: @Composable () -> Unit) {
    val light = lightColorScheme(primary = Color(0xFF356859), secondary = Color(0xFF5C8D78), background = Color(0xFFF7FBF8))
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else light, content = content)
}
