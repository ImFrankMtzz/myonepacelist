package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Arc
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.progress.EpisodeProgress
import com.myonepacelist.data.progress.WatchStatus

typealias Progress = Map<String, EpisodeProgress>

/** Share of an episode after which it counts as watched. */
const val WATCHED_THRESHOLD = 0.9f

/**
 * What to watch next: the most recently touched episode if it is still in progress,
 * otherwise the first unwatched episode after it. Nothing touched yet -> the very first episode.
 * Everything watched -> null.
 */
fun nextEpisode(catalog: Catalog, progress: Progress): Episode? {
    val all = catalog.episodes
    if (all.isEmpty()) return null
    val last = progress.entries
        .filter { catalog.episode(it.key) != null }
        .maxByOrNull { it.value.updatedAt }
        ?: return all.first()
    if (last.value.status == WatchStatus.IN_PROGRESS) return catalog.episode(last.key)
    val from = catalog.indexOf(last.key)
    return all.drop(from + 1).firstOrNull { progress[it.id]?.watched != true }
        ?: all.firstOrNull { progress[it.id]?.watched != true }
}

/** Marks every episode before [episodeId] (and itself when [inclusive]) as watched. */
fun markUpTo(catalog: Catalog, progress: Progress, episodeId: String, inclusive: Boolean, now: Long): Progress {
    val idx = catalog.indexOf(episodeId)
    require(idx >= 0) { "Unknown episode $episodeId" }
    val end = if (inclusive) idx else idx - 1
    val out = progress.toMutableMap()
    catalog.episodes.take(end + 1).forEachIndexed { i, ep ->
        val current = out[ep.id]
        if (current?.watched != true) {
            // Stagger timestamps so "most recent" still points at the last one marked.
            out[ep.id] = EpisodeProgress(
                status = WatchStatus.WATCHED,
                language = current?.language,
                updatedAt = now - (end - i),
                watchedAt = now,
            )
        }
    }
    return out
}

fun setWatched(progress: Progress, episodeId: String, watched: Boolean, now: Long): Progress =
    if (watched) progress + (episodeId to EpisodeProgress(WatchStatus.WATCHED, updatedAt = now, watchedAt = now, language = progress[episodeId]?.language))
    else progress - episodeId

/** Saves a playback position; crossing [WATCHED_THRESHOLD] marks the episode as watched. */
fun recordPosition(
    progress: Progress,
    episodeId: String,
    positionMs: Long,
    durationMs: Long,
    language: Language,
    now: Long,
): Progress {
    val prev = progress[episodeId]
    val done = durationMs > 0 && positionMs >= durationMs * WATCHED_THRESHOLD
    val watched = done || prev?.watched == true
    return progress + (episodeId to EpisodeProgress(
        status = if (watched) WatchStatus.WATCHED else WatchStatus.IN_PROGRESS,
        positionMs = if (done) 0 else positionMs,
        durationMs = durationMs,
        language = language,
        updatedAt = now,
        watchedAt = if (done && prev?.watched != true) now else prev?.watchedAt,
    ))
}

data class ArcProgress(val arc: Arc, val watched: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else watched.toFloat() / total
    val complete: Boolean get() = total > 0 && watched == total
}

data class Stats(
    val watched: Int,
    val total: Int,
    val watchedMinutes: Int,
    val remainingMinutes: Int,
    val arcsCompleted: Int,
    val arcs: List<ArcProgress>,
) {
    val fraction: Float get() = if (total == 0) 0f else watched.toFloat() / total
}

fun arcProgress(arc: Arc, progress: Progress) =
    ArcProgress(arc, arc.episodes.count { progress[it.id]?.watched == true }, arc.episodes.size)

fun stats(catalog: Catalog, progress: Progress): Stats {
    var watchedMin = 0.0
    var remainingMin = 0.0
    for (ep in catalog.episodes) {
        val p = progress[ep.id]
        when {
            p?.watched == true -> watchedMin += ep.durationMin
            p != null -> {
                watchedMin += ep.durationMin * p.fraction
                remainingMin += ep.durationMin * (1 - p.fraction)
            }
            else -> remainingMin += ep.durationMin
        }
    }
    val arcs = catalog.arcs.map { arcProgress(it, progress) }
    return Stats(
        watched = catalog.episodes.count { progress[it.id]?.watched == true },
        total = catalog.episodes.size,
        watchedMinutes = watchedMin.toInt(),
        remainingMinutes = remainingMin.toInt(),
        arcsCompleted = arcs.count { it.complete },
        arcs = arcs,
    )
}

data class CatalogDiff(val newEpisodes: List<Episode>, val newDubs: List<Episode>) {
    val isEmpty: Boolean get() = newEpisodes.isEmpty() && newDubs.isEmpty()
}

/** What appeared in [new] compared with [old]: brand-new episodes and episodes that just got a Spanish dub. */
fun catalogDiff(old: Catalog, new: Catalog): CatalogDiff {
    val newEps = new.episodes.filter { old.episode(it.id) == null }
    val newDubs = new.episodes.filter { ep -> ep.dub != null && old.episode(ep.id)?.let { it.dub == null } == true }
    return CatalogDiff(newEps, newDubs)
}

/** "3 h 25 min" / "45 min" */
fun formatMinutes(min: Int): String = when {
    min >= 60 && min % 60 != 0 -> "${min / 60} h ${min % 60} min"
    min >= 60 -> "${min / 60} h"
    else -> "$min min"
}
