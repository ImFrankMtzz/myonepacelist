package com.myonepacelist.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.player.PlayerActivity

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

fun Context.playEpisode(episodeId: String, language: Language, quality: Quality) {
    startActivity(PlayerActivity.intent(this, episodeId, language, quality))
}
