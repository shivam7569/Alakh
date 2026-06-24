package com.andy.alakh.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState

/**
 * A two-page swipeable session shell: [content] on the left, the live workout monitor on the right,
 * with subtle pager dots at the bottom. Used by both the workout list and the set-logging screen so
 * the monitor is reachable the same way (swipe right) from each.
 */
@Composable
fun PagerWithMonitor(content: @Composable () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) content() else WorkoutMonitorScreen()
        }
        PageDots(
            count = 2,
            current = pagerState.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
        )
    }
}

/** Subtle pager dots: the current page filled, others a hollow circle (a page sits to the right). */
@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            if (i == current) {
                Box(Modifier.size(6.dp).background(Color.White, CircleShape))
            } else {
                Box(Modifier.size(6.dp).border(1.dp, Color(0x80FFFFFF), CircleShape))
            }
        }
    }
}
