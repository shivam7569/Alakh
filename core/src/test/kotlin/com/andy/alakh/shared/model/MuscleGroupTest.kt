package com.andy.alakh.shared.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MuscleGroupTest {

    @Test
    fun fullBody_isExcludedFromTheRadar() {
        // The spider chart only plots specific muscles; the catch-all conditioning bucket is off-axis.
        assertThat(MuscleGroup.FULL_BODY.onRadar).isFalse()
    }

    @Test
    fun everySpecificMuscleIsOnTheRadar() {
        val offRadar = MuscleGroup.entries.filter { !it.onRadar }
        assertThat(offRadar).containsExactly(MuscleGroup.FULL_BODY)
    }

    @Test
    fun radarHasFourteenAxes() {
        // Kept in the readable ~8–14 range so the chart doesn't get cluttered.
        assertThat(MuscleGroup.entries.count { it.onRadar }).isEqualTo(14)
    }

    @Test
    fun displayNamesAreHumanReadable() {
        assertThat(MuscleGroup.CHEST.displayName).isEqualTo("Chest")
        assertThat(MuscleGroup.LOWER_BACK.displayName).isEqualTo("Lower back")
        assertThat(MuscleGroup.FULL_BODY.displayName).isEqualTo("Full body")
    }
}
