package com.andy.alakh.presentation.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide rest countdown between sets. Auto-started when a set is logged; the workout screen
 * shows a banner while it runs and buzzes when it reaches zero. `remaining == 0` means idle/done.
 */
object RestTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _remaining = MutableStateFlow(0)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    /** Last duration used, so "rest again" and the banner default stay consistent. */
    var durationSec = 90
        private set

    fun start(seconds: Int = durationSec) {
        durationSec = seconds.coerceAtLeast(5)
        job?.cancel()
        _remaining.value = durationSec
        job = scope.launch {
            while (_remaining.value > 0) {
                delay(1000)
                _remaining.value -= 1
            }
        }
    }

    /** Nudge the running countdown (e.g. +15s / −15s); ignored when idle. */
    fun addTime(deltaSec: Int) {
        if (_remaining.value > 0) _remaining.value = (_remaining.value + deltaSec).coerceAtLeast(0)
    }

    fun skip() {
        job?.cancel()
        _remaining.value = 0
    }
}
