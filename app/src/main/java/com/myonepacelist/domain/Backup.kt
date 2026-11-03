package com.myonepacelist.domain

import com.myonepacelist.data.progress.AppState
import com.myonepacelist.data.progress.EpisodeProgress
import com.myonepacelist.data.progress.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Portable progress file (JSON) the user can save anywhere and import on another phone. */
@Serializable
data class Backup(
    val app: String = APP_ID,
    val schema: Int = SCHEMA,
    val exportedAt: Long,
    val progress: Map<String, EpisodeProgress>,
    val settings: Settings,
) {
    companion object {
        const val APP_ID = "MyOnePaceList"
        const val SCHEMA = 1
    }
}

object BackupCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun export(state: AppState, now: Long): String =
        json.encodeToString(Backup.serializer(), Backup(exportedAt = now, progress = state.progress, settings = state.settings))

    /** @throws IllegalArgumentException when the file is not a MyOnePaceList backup. */
    fun decode(text: String): Backup {
        val b = runCatching { json.decodeFromString(Backup.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("El archivo no es un respaldo válido", it) }
        require(b.app == Backup.APP_ID) { "El archivo no es un respaldo de MyOnePaceList" }
        require(b.schema <= Backup.SCHEMA) { "Respaldo de una versión más nueva de la app" }
        return b
    }

    /** Merges [backup] into [state]: per episode, the most recently updated entry wins. */
    fun merge(state: AppState, backup: Backup): AppState {
        val merged = state.progress.toMutableMap()
        for ((id, p) in backup.progress) {
            val mine = merged[id]
            if (mine == null || p.updatedAt > mine.updatedAt) merged[id] = p
        }
        return state.copy(onboarded = true, progress = merged, settings = backup.settings)
    }
}
