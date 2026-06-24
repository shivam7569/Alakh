package com.andy.alakh.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF34C796)
private val GreenDeep = Color(0xFF1E6E59)

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF06281E),
    secondary = Green,
)

private val LightColors = lightColorScheme(
    primary = GreenDeep,
    onPrimary = Color.White,
    secondary = GreenDeep,
)

/** The phone companion's Material3 theme — Alakh green seeds the color scheme. */
@Composable
fun AlakhPhoneTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
