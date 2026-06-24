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

    /** The default rest length, also used as the banner default for the next set. */
    var durationSec = 90
        private set

    /** Set the preferred default (e.g. loaded from saved settings) without starting a countdown. */
    fun setDefaultDuration(seconds: Int) {
        durationSec = seconds.coerceIn(15, 600)
    }

    fun start(seconds: Int = durationSec) {
        durationSec = seconds.coerceIn(15, 600)
        job?.cancel()
        _remaining.value = durationSec
        job = scope.launch {
            while (_remaining.value > 0) {
                delay(1000)
                _remaining.value -= 1
            }
        }
    }

    /**
     * Adjust rest length by ±15s. Changes BOTH the running countdown and the saved default, so
     * "I want longer/shorter rests" sticks for every following set.
     */
    fun addTime(deltaSec: Int) {
        durationSec = (durationSec + deltaSec).coerceIn(15, 600)
        if (_remaining.value > 0) _remaining.value = (_remaining.value + deltaSec).coerceIn(1, 600)
    }

    fun skip() {
        job?.cancel()
        _remaining.value = 0
    }
}
