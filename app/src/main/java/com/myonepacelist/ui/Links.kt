package com.myonepacelist.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.myonepacelist.domain.BackupCodec
import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.player.PlayLauncherActivity

/** Opens a page in a Chrome Custom Tab (falls back to any browser). */
fun Context.openInBrowser(url: String) {
    val tab = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(0xFF0E1116.toInt()).build())
        .setShowTitle(true)
        .build()
    try {
        tab.launchUrl(this, url.toUri())
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Reads a backup the user picked, refusing huge files instead of loading them in memory. */
fun Context.readBackupFile(uri: Uri): String {
    val limit = BackupCodec.MAX_CHARS
    val bytes = contentResolver.openInputStream(uri)!!.use { it.readNBytesCompat(limit + 1) }
    require(bytes.size <= limit) { "El archivo es demasiado grande para ser un respaldo" }
    return bytes.decodeToString()
}

private fun java.io.InputStream.readNBytesCompat(n: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < n) {
        val r = read(buf, 0, minOf(buf.size, n - out.size()))
        if (r < 0) break
        out.write(buf, 0, r)
    }
    return out.toByteArray()
}

/** Plays an episode honoring the settings (mobile data, external player). */
fun Context.playEpisode(episodeId: String, language: Language, quality: Quality) {
    startActivity(PlayLauncherActivity.intent(this, episodeId, language, quality))
}
