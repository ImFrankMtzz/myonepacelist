package com.myonepacelist.data.catalog

import kotlinx.serialization.Serializable
import java.net.URLEncoder

/** Mirror of `catalog.json` produced by tools/catalog. */
@Serializable
data class Catalog(
    val schema: Int,
    val version: String,
    val generatedAt: String = "",
    val source: String = "",
    val arcs: List<Arc>,
) {
    /** Every episode in watch order. */
    val episodes: List<Episode> by lazy { arcs.flatMap { it.episodes } }
    private val byId: Map<String, Episode> by lazy { episodes.associateBy { it.id } }
    private val arcsBySlug: Map<String, Arc> by lazy { arcs.associateBy { it.slug } }

    fun episode(id: String): Episode? = byId[id]
    fun arc(slug: String): Arc? = arcsBySlug[slug]
    fun indexOf(id: String): Int = episodes.indexOfFirst { it.id == id }
    fun next(id: String): Episode? {
        val i = indexOf(id)
        return if (i < 0) null else episodes.getOrNull(i + 1)
    }

    companion object {
        const val SCHEMA = 1
    }
}

@Serializable
data class Arc(
    val slug: String,
    val title: String,
    val description: String = "",
    val special: Boolean = false,
    val unfinished: Boolean = false,
    val chapters: String = "",
    val animeEpisodes: String = "",
    val durationMin: Int = 0,
    val image: String? = null,
    val unreleased: Int = 0,
    val episodes: List<Episode> = emptyList(),
) {
    val hasDub: Boolean get() = episodes.any { it.dub != null }
}

@Serializable
data class Episode(
    val id: String,
    val arc: String,
    /** Usually whole, but One Pace has in-between parts such as Loguetown 01.5. */
    val part: Double,
    val title: String,
    val description: String = "",
    val special: Boolean = false,
    val toBeRedone: Boolean = false,
    val chapters: String = "",
    val animeEpisodes: String = "",
    val durationMin: Int = 0,
    val image: String? = null,
    val sub: Map<String, String>? = null,
    val dub: Map<String, String>? = null,
    val extended: Extended? = null,
) {
    /** "01", "02", "01.5"… as shown on onepace.net. */
    val number: String
        get() {
            val whole = part.toLong()
            val base = whole.toString().padStart(2, '0')
            return if (part == whole.toDouble()) base else base + part.toString().substringAfter('.').let { ".$it" }
        }

    fun files(language: Language): Map<String, String>? = when (language) {
        Language.DUB -> dub
        Language.SUB -> sub
    }

    fun available(language: Language) = files(language) != null
    val hasSpanish: Boolean get() = sub != null || dub != null

    /** The quality that will actually play: [quality] if available, else the closest one (ties go lower). */
    fun resolveQuality(language: Language, quality: Quality): Quality {
        val f = files(language) ?: return quality
        return Quality.entries
            .sortedWith(compareBy({ kotlin.math.abs(it.ordinal - quality.ordinal) }, { it.ordinal }))
            .firstOrNull { f.containsKey(it.key) } ?: quality
    }

    /** Pixeldrain id for the wanted quality, falling back to the closest one available. */
    fun fileId(language: Language, quality: Quality): String? = files(language)?.get(resolveQuality(language, quality).key)

    /** The first language available, preferring [preferred]. */
    fun resolveLanguage(preferred: Language): Language? =
        if (available(preferred)) preferred else Language.entries.firstOrNull { available(it) }
}

@Serializable
data class Extended(val sub: Map<String, String>? = null, val dub: Map<String, String>? = null)

@Serializable
enum class Language { DUB, SUB }

@Serializable
enum class Quality(val key: String) { Q480("480"), Q720("720"), Q1080("1080") }

object OnePaceLinks {
    private const val BASE = "https://onepace.net"

    /** Resized image through onepace.net's own optimizer (widths it serves: 640, 750, 828, 1080, 1200, 1920). */
    fun image(src: String?, width: Int = 640): String? = src?.let {
        "$BASE/_next/image?url=${URLEncoder.encode(it, "UTF-8")}&w=$width&q=75"
    }

    fun episodePage(id: String) = "$BASE/es-419/watch#$id"
    fun pixeldrainStream(fileId: String) = "https://pixeldrain.net/api/file/$fileId"
    fun pixeldrainViewer(fileId: String) = "https://pixeldrain.net/u/$fileId"
}
