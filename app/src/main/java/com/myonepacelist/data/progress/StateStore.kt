package com.myonepacelist.data.progress

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private object AppStateSerializer : Serializer<AppState> {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    override val defaultValue = AppState()
    override suspend fun readFrom(input: InputStream): AppState = try {
        json.decodeFromString(AppState.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Estado ilegible", e)
    }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(json.encodeToString(AppState.serializer(), t).encodeToByteArray())
    }
}

/** Local persistence of progress and settings. Single JSON file, atomic writes via DataStore. */
class StateStore(context: Context) {
    private val store: DataStore<AppState> = DataStoreFactory.create(
        serializer = AppStateSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { AppState() },
        produceFile = { File(context.filesDir, "datastore/state.json") },
    )

    val state: Flow<AppState> = store.data

    suspend fun current(): AppState = store.data.first()
    suspend fun update(transform: (AppState) -> AppState): AppState = store.updateData(transform)
}
