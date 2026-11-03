package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.progress.EpisodeProgress
import com.myonepacelist.data.progress.Settings
import com.myonepacelist.data.progress.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTest {
    private val base = Settings()

    @Test fun `plays inside the app by default, even on data`() {
        assertEquals(PlaybackRoute.INTERNAL, playbackRoute(base, metered = true))
    }

    @Test fun `asks before using mobile data when wifi only is on`() {
        val s = base.copy(wifiOnly = true)
        assertEquals(PlaybackRoute.ASK_DATA, playbackRoute(s, metered = true))
        assertEquals(PlaybackRoute.INTERNAL, playbackRoute(s, metered = false))
        assertEquals(PlaybackRoute.INTERNAL, playbackRoute(s, metered = true, allowData = true))
    }

    @Test fun `external player is used once data is allowed`() {
        val s = base.copy(wifiOnly = true, externalPlayer = true)
        assertEquals(PlaybackRoute.ASK_DATA, playbackRoute(s, metered = true))
        assertEquals(PlaybackRoute.EXTERNAL, playbackRoute(s, metered = true, allowData = true))
        assertEquals(PlaybackRoute.EXTERNAL, playbackRoute(s, metered = false))
    }

    @Test fun `built-in player pauses on data only when the user did not allow it`() {
        val s = base.copy(wifiOnly = true)
        assertTrue(shouldPauseForData(s, metered = true, allowData = false))
        assertFalse(shouldPauseForData(s, metered = true, allowData = true))
        assertFalse(shouldPauseForData(s, metered = false, allowData = false))
        assertFalse(shouldPauseForData(base, metered = true, allowData = false))
    }

    @Test fun `external result drops bogus numbers and clamps position`() {
        assertEquals(ExternalResult(null, null, false), externalResult(-5, -1, null))
        assertEquals(ExternalResult(null, null, false), externalResult(Long.MAX_VALUE, Long.MAX_VALUE, "user"))
        assertEquals(ExternalResult(1_000, 1_000, false), externalResult(9_999, 1_000, "user"))
        assertTrue(externalResult(null, null, "playback_completion").completed)
    }

    @Test fun `external position is saved like the built-in player`() {
        val r = externalResult(600_000, 1_500_000, "user")
        val p = applyExternalResult(emptyMap(), "e", r, 0, Language.SUB, now = 10)
        assertEquals(WatchStatus.IN_PROGRESS, p["e"]?.status)
        assertEquals(600_000L, p["e"]?.positionMs)
        assertEquals(Language.SUB, p["e"]?.language)
    }

    @Test fun `external completion marks watched, using the catalog duration if needed`() {
        val p = applyExternalResult(emptyMap(), "e", externalResult(null, null, "playback_completion"), 1_200_000, Language.DUB, 10)
        assertTrue(p["e"]!!.watched)
        val q = applyExternalResult(emptyMap(), "e", externalResult(null, null, "playback_completion"), 0, Language.DUB, 10)
        assertTrue(q["e"]!!.watched)
    }

    @Test fun `no report only touches the episode and keeps its saved minute`() {
        val prev = mapOf("e" to EpisodeProgress(WatchStatus.IN_PROGRESS, positionMs = 300_000, durationMs = 1_500_000, updatedAt = 1))
        val p = applyExternalResult(prev, "e", externalResult(null, null, null), 1_500_000, Language.DUB, now = 50)
        assertEquals(300_000L, p["e"]?.positionMs)
        assertEquals(50L, p["e"]?.updatedAt)
        val fresh = applyExternalResult(emptyMap(), "e", externalResult(null, null, null), 0, Language.DUB, now = 50)
        assertEquals(WatchStatus.IN_PROGRESS, fresh["e"]?.status)
        assertNull(fresh["e"]?.watchedAt)
    }
}
