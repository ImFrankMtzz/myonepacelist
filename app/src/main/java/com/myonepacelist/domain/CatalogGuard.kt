package com.myonepacelist.domain

import com.myonepacelist.data.catalog.Arc
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Extended

/**
 * The catalog is downloaded from GitHub and its values end up inside URLs (Pixeldrain, the
 * onepace.net image optimizer) and intents for other apps. Anything that doesn't look like what
 * One Pace publishes is dropped, so a tampered catalog can't point the app somewhere else.
 */
object CatalogGuard {
    private val FILE_ID = Regex("[A-Za-z0-9]{4,32}")
    private val SLUG = Regex("[a-z0-9][a-z0-9.-]{0,95}")

    fun isFileId(s: String) = FILE_ID.matches(s)
    fun isSlug(s: String) = SLUG.matches(s)

    /** Only same-site paths for the onepace.net optimizer: "/_next/static/…", never "//host" or "..". */
    fun isImagePath(s: String) = s.startsWith("/") && !s.startsWith("//") && ".." !in s && s.none { it.isWhitespace() || it == '\\' }

    private fun files(m: Map<String, String>?): Map<String, String>? =
        m?.filter { (res, id) -> res in RESOLUTIONS && isFileId(id) }?.takeIf { it.isNotEmpty() }

    private fun image(s: String?) = s?.takeIf { isImagePath(it) }

    private fun episode(e: Episode, arcSlug: String): Episode? {
        if (!isSlug(e.id) || e.arc != arcSlug) return null
        val ext = e.extended?.let { Extended(files(it.sub), files(it.dub)) }?.takeIf { it.sub != null || it.dub != null }
        return e.copy(sub = files(e.sub), dub = files(e.dub), extended = ext, image = image(e.image))
    }

    /** Returns a catalog with every unsafe value removed (or the arc/episode that carried it). */
    fun sanitize(c: Catalog): Catalog {
        val seen = HashSet<String>()
        val arcs = c.arcs.filter { isSlug(it.slug) }.map { a: Arc ->
            a.copy(image = image(a.image), episodes = a.episodes.mapNotNull { episode(it, a.slug) }.filter { seen.add(it.id) })
        }
        return c.copy(arcs = arcs)
    }

    private val RESOLUTIONS = setOf("480", "720", "1080")
}
