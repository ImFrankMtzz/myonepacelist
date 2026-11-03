package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.progress.AppState
import com.myonepacelist.data.progress.EpisodeProgress
import com.myonepacelist.data.progress.Settings
import com.myonepacelist.data.progress.WatchStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val state = AppState(
        onboarded = true,
        progress = mapOf(
            "a-01" to EpisodeProgress(WatchStatus.WATCHED, updatedAt = 10, watchedAt = 10),
            "a-02" to EpisodeProgress(WatchStatus.IN_PROGRESS, 1000, 2000, Language.SUB, updatedAt = 20),
        ),
        settings = Settings(language = Language.SUB),
    )

    @Test fun `export then import round-trips`() {
        val text = BackupCodec.export(state, now = 99)
        val merged = BackupCodec.merge(AppState(), BackupCodec.decode(text))
        assertEquals(state.progress, merged.progress)
        assertEquals(state.settings, merged.settings)
        assertTrue(merged.onboarded)
    }

    @Test fun `merge keeps the newest entry per episode`() {
        val older = BackupCodec.decode(BackupCodec.export(state, 1))
        val mine = AppState(progress = mapOf("a-02" to EpisodeProgress(WatchStatus.WATCHED, updatedAt = 30)))
        val merged = BackupCodec.merge(mine, older)
        assertEquals(WatchStatus.WATCHED, merged.progress["a-02"]?.status)
        assertEquals(2, merged.progress.size)
    }

    @Test fun `rejects files that are not backups`() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("{\"hola\":1}") }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("no json") }
    }

    @Test fun `imported entries are sanitized`() {
        val text = """{"app":"MyOnePaceList","schema":1,"exportedAt":1,"settings":{},"progress":{
            "a-01":{"status":"IN_PROGRESS","positionMs":-50,"durationMs":1000,"updatedAt":1},
            "a-02":{"status":"IN_PROGRESS","positionMs":99999999999,"durationMs":2000,"updatedAt":1},
            "../evil":{"status":"WATCHED","updatedAt":1}}}"""
        val b = BackupCodec.decode(text)
        assertEquals(setOf("a-01", "a-02"), b.progress.keys)
        assertEquals(0L, b.progress["a-01"]?.positionMs)
        assertEquals(2000L, b.progress["a-02"]?.positionMs)
    }

    @Test fun `refuses huge files`() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(" ".repeat(BackupCodec.MAX_CHARS + 1)) }
    }

    @Test fun `bundled catalog parses and has spanish sources`() {
        val file = listOf(File("src/main/assets/catalog.json"), File("app/src/main/assets/catalog.json")).first { it.exists() }
        val catalog = json.decodeFromString(Catalog.serializer(), file.readText())
        assertEquals(Catalog.SCHEMA, catalog.schema)
        assertTrue(catalog.arcs.size >= 10)
        assertTrue(catalog.episodes.count { it.dub != null } > 10)
        assertEquals("romance-dawn-01", catalog.episodes.first().id)
        assertEquals("01.5", catalog.episode("loguetown-01.5")?.number)
    }
}
