package com.andy.alakh.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

/**
 * App theme wrapper. Uses the Wear Material3 defaults for now; customize the
 * colorScheme / typography here when you want Alakh's own look.
 */
@Composable
fun AlakhTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
