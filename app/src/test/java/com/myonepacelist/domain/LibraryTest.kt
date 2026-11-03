package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Arc
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.data.progress.EpisodeProgress
import com.myonepacelist.data.progress.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTest {
    private fun ep(arc: String, part: Int, dub: Boolean = true, min: Int = 20) = Episode(
        id = "$arc-${part.toString().padStart(2, '0')}", arc = arc, part = part.toDouble(), title = "t$part", durationMin = min,
        sub = mapOf("480" to "s4", "720" to "s7", "1080" to "s1"),
        dub = if (dub) mapOf("720" to "d7") else null,
    )

    private val catalog = Catalog(
        schema = 1, version = "v1",
        arcs = listOf(
            Arc("a", "A", episodes = listOf(ep("a", 1), ep("a", 2), ep("a", 3))),
            Arc("b", "B", episodes = listOf(ep("b", 1, dub = false), ep("b", 2))),
        ),
    )
    private fun watched(at: Long) = EpisodeProgress(WatchStatus.WATCHED, updatedAt = at, watchedAt = at)

    @Test fun `next is the first episode when nothing was watched`() {
        assertEquals("a-01", nextEpisode(catalog, emptyMap())?.id)
    }

    @Test fun `next follows the most recently watched episode`() {
        val p = mapOf("a-01" to watched(1), "a-02" to watched(2))
        assertEquals("a-03", nextEpisode(catalog, p)?.id)
    }

    @Test fun `in-progress episode wins when it is the latest touched`() {
        val p = mapOf("a-01" to watched(1), "b-01" to EpisodeProgress(WatchStatus.IN_PROGRESS, 1000, 5000, updatedAt = 5))
        assertEquals("b-01", nextEpisode(catalog, p)?.id)
    }

    @Test fun `next skips already watched episodes and wraps to gaps`() {
        val p = mapOf("a-03" to watched(1), "b-01" to watched(2), "b-02" to watched(3))
        assertEquals("a-01", nextEpisode(catalog, p)?.id)
        val all = catalog.episodes.associate { it.id to watched(1) }
        assertNull(nextEpisode(catalog, all))
    }

    @Test fun `markUpTo exclusive means I am about to watch this one`() {
        val p = markUpTo(catalog, emptyMap(), "b-01", inclusive = false, now = 100)
        assertEquals(setOf("a-01", "a-02", "a-03"), p.keys)
        assertTrue(p.values.all { it.watched })
        assertEquals("b-01", nextEpisode(catalog, p)?.id)
    }

    @Test fun `markUpTo inclusive marks the target too`() {
        val p = markUpTo(catalog, emptyMap(), "a-02", inclusive = true, now = 100)
        assertEquals(setOf("a-01", "a-02"), p.keys)
        assertEquals("a-03", nextEpisode(catalog, p)?.id)
    }

    @Test fun `recordPosition marks watched after 90 percent`() {
        var p = recordPosition(emptyMap(), "a-01", 500, 1000, Language.DUB, now = 1)
        assertEquals(WatchStatus.IN_PROGRESS, p["a-01"]?.status)
        assertEquals(0.5f, p["a-01"]!!.fraction)
        p = recordPosition(p, "a-01", 950, 1000, Language.DUB, now = 2)
        assertEquals(WatchStatus.WATCHED, p["a-01"]?.status)
        assertEquals(2L, p["a-01"]?.watchedAt)
        // Rewatching keeps it watched.
        p = recordPosition(p, "a-01", 100, 1000, Language.SUB, now = 3)
        assertEquals(WatchStatus.WATCHED, p["a-01"]?.status)
        assertEquals(2L, p["a-01"]?.watchedAt)
    }

    @Test fun `stats count episodes minutes and arcs`() {
        val p = mapOf(
            "a-01" to watched(1), "a-02" to watched(1), "a-03" to watched(1),
            "b-01" to EpisodeProgress(WatchStatus.IN_PROGRESS, 500, 1000, updatedAt = 2),
        )
        val s = stats(catalog, p)
        assertEquals(3, s.watched)
        assertEquals(5, s.total)
        assertEquals(70, s.watchedMinutes)
        assertEquals(30, s.remainingMinutes)
        assertEquals(1, s.arcsCompleted)
        assertEquals(1f, s.arcs[0].fraction)
    }

    @Test fun `diff finds new episodes and new dubs`() {
        val newCatalog = catalog.copy(
            version = "v2",
            arcs = catalog.arcs.map { arc ->
                if (arc.slug == "b") arc.copy(episodes = listOf(ep("b", 1, dub = true), ep("b", 2), ep("b", 3)))
                else arc
            },
        )
        val d = catalogDiff(catalog, newCatalog)
        assertEquals(listOf("b-03"), d.newEpisodes.map { it.id })
        assertEquals(listOf("b-01"), d.newDubs.map { it.id })
    }

    @Test fun `fileId falls back to the closest quality and language resolves`() {
        val e = ep("a", 1)
        assertEquals("d7", e.fileId(Language.DUB, Quality.Q1080))
        assertEquals("s1", e.fileId(Language.SUB, Quality.Q1080))
        val noDub = ep("b", 1, dub = false)
        assertNull(noDub.fileId(Language.DUB, Quality.Q720))
        assertEquals(Language.SUB, noDub.resolveLanguage(Language.DUB))
    }

    @Test fun `episode numbers keep in-between parts`() {
        assertEquals("01", ep("a", 1).number)
        assertEquals("01.5", ep("a", 1).copy(part = 1.5).number)
        assertEquals("12", ep("a", 12).number)
    }

    @Test fun `format minutes`() {
        assertEquals("45 min", formatMinutes(45))
        assertEquals("2 h", formatMinutes(120))
        assertEquals("3 h 25 min", formatMinutes(205))
    }
}
