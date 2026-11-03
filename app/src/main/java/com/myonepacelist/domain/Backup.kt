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

    /** A real backup of all ~500 episodes is ~100 KB. */
    const val MAX_CHARS = 2_000_000
    private const val MAX_ENTRIES = 10_000
    private const val MAX_MS = 6L * 60 * 60 * 1000

    /**
     * @throws IllegalArgumentException when the file is not a MyOnePaceList backup.
     * The file comes from anywhere, so entries are sanitized: unknown-looking ids are dropped and
     * positions are clamped.
     */
    fun decode(text: String): Backup {
        require(text.length <= MAX_CHARS) { "El archivo es demasiado grande para ser un respaldo" }
        val b = runCatching { json.decodeFromString(Backup.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("El archivo no es un respaldo válido", it) }
        require(b.app == Backup.APP_ID) { "El archivo no es un respaldo de MyOnePaceList" }
        require(b.schema <= Backup.SCHEMA) { "Respaldo de una versión más nueva de la app" }
        require(b.progress.size <= MAX_ENTRIES) { "El respaldo tiene demasiadas entradas" }
        val progress = b.progress
            .filterKeys { CatalogGuard.isSlug(it) }
            .mapValues { (_, p) ->
                val dur = p.durationMs.coerceIn(0, MAX_MS)
                p.copy(durationMs = dur, positionMs = p.positionMs.coerceIn(0, if (dur > 0) dur else MAX_MS))
            }
        return b.copy(progress = progress)
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
