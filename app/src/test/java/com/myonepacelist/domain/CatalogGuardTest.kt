package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Arc
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Extended
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlinx.serialization.json.Json

class CatalogGuardTest {
    private fun ep(id: String, sub: Map<String, String>? = null, image: String? = null, arc: String = "a") =
        Episode(id = id, arc = arc, part = 1.0, title = id, sub = sub, image = image)

    private fun cat(vararg eps: Episode, slug: String = "a", image: String? = null) =
        Catalog(schema = 1, version = "v", arcs = listOf(Arc(slug, "A", image = image, episodes = eps.toList())))

    @Test fun `keeps what One Pace publishes`() {
        val c = cat(ep("romance-dawn-01", mapOf("720" to "AbC12345"), "/_next/static/media/x.jpg"), image = "/_next/static/media/a.jpg", slug = "romance-dawn")
            .let { it.copy(arcs = it.arcs.map { a -> a.copy(episodes = a.episodes.map { e -> e.copy(arc = "romance-dawn") }) }) }
        assertEquals(c, CatalogGuard.sanitize(c))
    }

    @Test fun `drops file ids that would change the Pixeldrain URL`() {
        val bad = listOf("../../user", "abc?x=1", "ab/cd", "a b c d", "@evil.com", "", "x".repeat(40))
        for (id in bad) {
            val e = CatalogGuard.sanitize(cat(ep("e", mapOf("720" to id)))).episodes.single()
            assertNull("$id must be dropped", e.sub)
        }
        val mixed = CatalogGuard.sanitize(cat(ep("e", mapOf("720" to "good1234", "1080" to "../x", "9999" to "good5678")))).episodes.single()
        assertEquals(mapOf("720" to "good1234"), mixed.sub)
    }

    @Test fun `drops images that are not onepace paths`() {
        for (src in listOf("//evil.com/x.jpg", "https://evil.com/x.jpg", "/a/../../x", "/a b", "\\\\host\\x")) {
            assertFalse(src, CatalogGuard.isImagePath(src))
        }
        assertTrue(CatalogGuard.isImagePath("/_next/static/media/drum.1a2b.jpg"))
        assertNull(CatalogGuard.sanitize(cat(ep("e", image = "//evil.com/x"))).episodes.single().image)
    }

    @Test fun `drops bad slugs, duplicates and mismatched arcs`() {
        val c = Catalog(
            schema = 1, version = "v",
            arcs = listOf(
                Arc("a", "A", episodes = listOf(ep("ok-01"), ep("Bad Id"), ep("ok-01"), ep("other", arc = "b"))),
                Arc("../x", "X", episodes = listOf(ep("x-01", arc = "../x"))),
            ),
        )
        val s = CatalogGuard.sanitize(c)
        assertEquals(listOf("a"), s.arcs.map { it.slug })
        assertEquals(listOf("ok-01"), s.episodes.map { it.id })
    }

    @Test fun `extended cuts are filtered too`() {
        val e = ep("e").copy(extended = Extended(sub = mapOf("720" to "../x"), dub = null))
        assertNull(CatalogGuard.sanitize(cat(e)).episodes.single().extended)
    }

    @Test fun `bundled catalog passes untouched`() {
        val f = listOf(File("src/main/assets/catalog.json"), File("app/src/main/assets/catalog.json")).first { it.exists() }
        val c = Json { ignoreUnknownKeys = true }.decodeFromString(Catalog.serializer(), f.readText())
        assertEquals(c, CatalogGuard.sanitize(c))
    }
}
