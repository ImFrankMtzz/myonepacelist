package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.progress.EpisodeProgress
import com.myonepacelist.data.progress.Settings
import com.myonepacelist.data.progress.WatchStatus

/** Where a "play" tap goes. */
enum class PlaybackRoute {
    /** The built-in Media3 player. */
    INTERNAL,
    /** Another video app chosen by the user (VLC, MX Player, mpv…). */
    EXTERNAL,
    /** On mobile data with "no reproducir con datos" on: ask before spending data. */
    ASK_DATA,
}

/**
 * Decides how to play. [metered] means the active network costs data (cellular, or a hotspot);
 * [allowData] is the user saying "play anyway" for this one episode.
 */
fun playbackRoute(settings: Settings, metered: Boolean, allowData: Boolean = false): PlaybackRoute = when {
    settings.wifiOnly && metered && !allowData -> PlaybackRoute.ASK_DATA
    settings.externalPlayer -> PlaybackRoute.EXTERNAL
    else -> PlaybackRoute.INTERNAL
}

/** True when the built-in player must pause because the network switched to mobile data. */
fun shouldPauseForData(settings: Settings, metered: Boolean, allowData: Boolean): Boolean =
    settings.wifiOnly && metered && !allowData

/** Longest believable episode (One Pace's longest are ~1 h). Anything above is a bogus report. */
private const val MAX_REPORTED_MS = 6L * 60 * 60 * 1000

/**
 * What an external player reported when it closed. Players follow MX Player's convention
 * (`position`, `duration`, `end_by`, also used by mpv) or VLC's (`extra_position`, `extra_duration`).
 * The values come from another app, so they are validated: negative or absurd numbers are dropped.
 */
data class ExternalResult(val positionMs: Long?, val durationMs: Long?, val completed: Boolean)

fun externalResult(position: Long?, duration: Long?, endBy: String?): ExternalResult {
    val dur = duration?.takeIf { it in 1..MAX_REPORTED_MS }
    val pos = position?.takeIf { it in 0..MAX_REPORTED_MS }?.let { p -> dur?.let { p.coerceAtMost(it) } ?: p }
    return ExternalResult(positionMs = pos, durationMs = dur, completed = endBy == "playback_completion")
}

/**
 * Saves what happened in an external player. [catalogDurationMs] (from the catalog, rounded)
 * stands in when the player doesn't report a duration. Without any position the episode is only
 * "touched": it becomes the one to continue, keeping its saved minute and watched state.
 */
fun applyExternalResult(
    progress: Progress,
    episodeId: String,
    result: ExternalResult,
    catalogDurationMs: Long,
    language: Language,
    now: Long,
): Progress {
    val dur = result.durationMs ?: catalogDurationMs.takeIf { it > 0 }
    return when {
        result.completed && dur != null -> recordPosition(progress, episodeId, dur, dur, language, now)
        result.completed -> setWatched(progress, episodeId, true, now)
        result.positionMs != null && result.positionMs >= 5_000 && dur != null ->
            recordPosition(progress, episodeId, result.positionMs, dur, language, now)
        else -> {
            val prev = progress[episodeId]
            progress + (episodeId to (prev?.copy(updatedAt = now, language = language)
                ?: EpisodeProgress(WatchStatus.IN_PROGRESS, language = language, updatedAt = now)))
        }
    }
}
