package com.andy.alakh.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * App theme. Overrides the Material primary so Buttons and other accented components pick up the
 * Alakh cyan automatically across the app.
 */
@Composable
fun AlakhTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = AlakhAccent,
            onPrimary = AlakhOnAccent,
        ),
        content = content,
    )
}
