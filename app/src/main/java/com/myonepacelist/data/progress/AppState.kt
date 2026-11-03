package com.myonepacelist.data.progress

import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import kotlinx.serialization.Serializable

@Serializable
enum class WatchStatus { IN_PROGRESS, WATCHED }

@Serializable
data class EpisodeProgress(
    val status: WatchStatus,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val language: Language? = null,
    val updatedAt: Long,
    val watchedAt: Long? = null,
) {
    val watched: Boolean get() = status == WatchStatus.WATCHED
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

@Serializable
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Serializable
data class Settings(
    val language: Language = Language.DUB,
    val quality: Quality = Quality.Q720,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val notifications: Boolean = false,
    val autoplayNext: Boolean = true,
)

/** Everything the app persists. Lives in a single DataStore file (no account, no server). */
@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val progress: Map<String, EpisodeProgress> = emptyMap(),
    val settings: Settings = Settings(),
    /** Catalog version the user has already been notified about. */
    val knownCatalogVersion: String? = null,
)
