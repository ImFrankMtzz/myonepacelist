package com.myonepacelist.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Catalog source of truth: the copy bundled in assets, replaced by a newer copy downloaded from
 * GitHub (regenerated weekly by the catalog Action) and cached on disk so it works offline.
 */
class CatalogRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val remoteUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile get() = File(context.filesDir, "catalog.json")
    private val _catalog = MutableStateFlow<Catalog?>(null)
    val catalog: StateFlow<Catalog?> = _catalog.asStateFlow()

    /** Loads the best local catalog (cached download if valid, else bundled). */
    suspend fun load(): Catalog = withContext(Dispatchers.IO) {
        _catalog.value?.let { return@withContext it }
        val bundled = decode(context.assets.open("catalog.json").bufferedReader().use { it.readText() })!!
        val cached = cacheFile.takeIf { it.exists() }?.let { decode(it.readText()) }
        val best = if (cached != null && cached.generatedAt > bundled.generatedAt) cached else bundled
        _catalog.value = best
        best
    }

    /**
     * Downloads the remote catalog. Returns (previous, new) when it changed, null when unchanged.
     * @throws java.io.IOException on network problems.
     */
    suspend fun refresh(): Pair<Catalog, Catalog>? = withContext(Dispatchers.IO) {
        val previous = load()
        val body = http.newCall(Request.Builder().url(remoteUrl).build()).execute().use { res ->
            if (!res.isSuccessful) throw java.io.IOException("HTTP ${res.code}")
            res.body.string()
        }
        val remote = decode(body) ?: throw java.io.IOException("Catálogo remoto inválido")
        if (remote.version == previous.version || remote.generatedAt < previous.generatedAt) return@withContext null
        cacheFile.writeText(body)
        _catalog.value = remote
        previous to remote
    }

    private fun decode(text: String): Catalog? = runCatching {
        json.decodeFromString(Catalog.serializer(), text)
    }.getOrNull()?.takeIf { it.schema == Catalog.SCHEMA && it.arcs.isNotEmpty() }
}
