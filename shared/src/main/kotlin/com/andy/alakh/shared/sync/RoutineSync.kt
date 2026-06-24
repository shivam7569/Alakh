package com.andy.alakh.shared.sync

import android.content.Context
import com.andy.alakh.shared.model.Routine
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Routine storage backed by the Wear Data Layer — which IS the shared store, not a cache: the phone
 * writes routine items, and BOTH the phone and the watch read them back. Each routine is one DataItem
 * at `/routine/<id>` holding the [RoutineCodec] JSON. Putting/deleting an item propagates to the
 * paired device automatically, so there's no separate database to keep in sync.
 */
object RoutineSync {
    const val PATH_PREFIX = "/routine/"

    private fun path(id: String) = "$PATH_PREFIX$id"

    /** Create or update a routine (and sync it to the paired device). */
    suspend fun putRoutine(context: Context, routine: Routine) = withContext(Dispatchers.IO) {
        val request = PutDataMapRequest.create(path(routine.id)).apply {
            dataMap.putString("json", RoutineCodec.encode(routine))
            dataMap.putLong("updatedAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Tasks.await(Wearable.getDataClient(context).putDataItem(request))
        Unit
    }

    /** All stored routines, newest names first-ish (alphabetical), decoded from their JSON. */
    suspend fun getRoutines(context: Context): List<Routine> = withContext(Dispatchers.IO) {
        val buffer = Tasks.await(Wearable.getDataClient(context).dataItems)
        try {
            buffer.mapNotNull { item ->
                if (item.uri.path?.startsWith(PATH_PREFIX) != true) return@mapNotNull null
                val json = DataMapItem.fromDataItem(item).dataMap.getString("json") ?: return@mapNotNull null
                runCatching { RoutineCodec.decode(json) }.getOrNull()
            }.sortedBy { it.name.lowercase() }
        } finally {
            buffer.release()
        }
    }

    suspend fun deleteRoutine(context: Context, id: String) = withContext(Dispatchers.IO) {
        val client = Wearable.getDataClient(context)
        val buffer = Tasks.await(client.dataItems)
        val uri = try {
            buffer.firstOrNull { it.uri.path == path(id) }?.uri
        } finally {
            buffer.release()
        }
        if (uri != null) Tasks.await(client.deleteDataItems(uri))
        Unit
    }
}
