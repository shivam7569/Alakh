package com.andy.alakh.shared.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoggedSetTest {

    @Test
    fun volumeLoad_isWeightTimesReps() {
        assertThat(LoggedSet(weightKg = 60.0, reps = 8).volumeLoad).isEqualTo(480.0)
    }

    @Test
    fun volumeLoad_isZeroWhenWeightMissing() {
        // e.g. a bodyweight or timed set — no barbell load to count.
        assertThat(LoggedSet(weightKg = null, reps = 10).volumeLoad).isEqualTo(0.0)
    }

    @Test
    fun volumeLoad_isZeroWhenRepsMissing() {
        assertThat(LoggedSet(weightKg = 50.0, reps = null).volumeLoad).isEqualTo(0.0)
    }

    @Test
    fun defaults_areAnIncompleteNormalSet() {
        val set = LoggedSet()
        assertThat(set.setType).isEqualTo(SetType.NORMAL)
        assertThat(set.completed).isFalse()
        assertThat(set.volumeLoad).isEqualTo(0.0)
    }
}
