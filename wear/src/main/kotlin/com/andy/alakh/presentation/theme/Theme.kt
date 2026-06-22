package com.andy.alakh.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * App theme. We keep the default Material color scheme (so component backgrounds stay as-is); the
 * Alakh accent is applied only as a font/highlight color where we use it explicitly.
 */
@Composable
fun AlakhTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
