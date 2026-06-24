package com.andy.alakh.shared.sync

import com.andy.alakh.shared.model.Routine
import kotlinx.serialization.json.Json

/**
 * Encodes/decodes a [Routine] to JSON — the payload that travels over the Wear Data Layer between
 * the phone and the watch. Pure Kotlin (lives in :core) so the round-trip is unit-tested on CI.
 */
object RoutineCodec {
    private val json = Json {
        ignoreUnknownKeys = true // forward-compatible: tolerate fields a newer app version may add
        encodeDefaults = true
    }

    fun encode(routine: Routine): String = json.encodeToString(Routine.serializer(), routine)

    fun decode(text: String): Routine = json.decodeFromString(Routine.serializer(), text)
}
