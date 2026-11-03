package com.myonepacelist.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.myonepacelist.container
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.OnePaceLinks
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.data.progress.Settings
import com.myonepacelist.data.progress.WatchStatus
import com.myonepacelist.domain.PlaybackRoute
import com.myonepacelist.domain.WATCHED_THRESHOLD
import com.myonepacelist.domain.applyExternalResult
import com.myonepacelist.domain.externalResult
import com.myonepacelist.domain.playbackRoute
import com.myonepacelist.ui.theme.MyOnePaceTheme
import com.myonepacelist.ui.theme.Tokens
import kotlinx.coroutines.launch

/**
 * Invisible entry point for every "play" tap (app and widget). It applies the playback settings
 * at the moment of the tap: asks before using mobile data, then opens the built-in player or the
 * user's external video app, and saves what the external app reports back.
 */
class PlayLauncherActivity : ComponentActivity() {
    private lateinit var episodeId: String
    private lateinit var language: Language
    private lateinit var quality: Quality

    /** Language actually sent to the external player; kept across recreation for the result. */
    private var externalLanguage: Language? = null

    private val external = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::onExternalResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        episodeId = intent.getStringExtra(EXTRA_EPISODE) ?: return finish()
        language = runCatching { Language.valueOf(intent.getStringExtra(EXTRA_LANGUAGE)!!) }.getOrDefault(Language.DUB)
        quality = runCatching { Quality.valueOf(intent.getStringExtra(EXTRA_QUALITY)!!) }.getOrDefault(Quality.Q720)
        externalLanguage = savedInstanceState?.getString(STATE_EXTERNAL)?.let { runCatching { Language.valueOf(it) }.getOrNull() }
        // Recreated while the external player is open: just wait for its result.
        if (externalLanguage != null) return
        lifecycleScope.launch { route(container.state.current().settings, allowData = false) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        externalLanguage?.let { outState.putString(STATE_EXTERNAL, it.name) }
    }

    private suspend fun route(settings: Settings, allowData: Boolean) {
        when (playbackRoute(settings, isOnMeteredNetwork(), allowData)) {
            PlaybackRoute.ASK_DATA -> askData(settings)
            PlaybackRoute.INTERNAL -> openInternal(allowData)
            PlaybackRoute.EXTERNAL -> openExternal(allowData)
        }
    }

    private fun askData(settings: Settings) = setContent {
        MyOnePaceTheme(settings.theme) {
            AlertDialog(
                onDismissRequest = { finish() },
                title = { Text("Estás usando datos móviles") },
                text = { Text("Pediste no reproducir con datos. Un episodio en ${quality.key}p puede gastar cientos de MB.") },
                confirmButton = {
                    TextButton(onClick = { lifecycleScope.launch { route(settings, allowData = true) } }) {
                        Text("Reproducir igual", color = Tokens.accent)
                    }
                },
                dismissButton = { TextButton(onClick = { finish() }) { Text("Cancelar", color = Tokens.text) } },
                containerColor = Tokens.surface,
                titleContentColor = Tokens.text,
                textContentColor = Tokens.textSoft,
            )
        }
    }

    private fun openInternal(allowData: Boolean) {
        startActivity(PlayerActivity.intent(this, episodeId, language, quality, allowData))
        finish()
    }

    private suspend fun openExternal(allowData: Boolean) {
        val ep = container.catalog.load().episode(episodeId)
        val lang = ep?.resolveLanguage(language)
        val fileId = lang?.let { ep.fileId(it, quality) }
        if (ep == null || lang == null || fileId == null) return openInternal(allowData) // the built-in player explains the problem
        val saved = container.state.current().progress[ep.id]
        val resumeAt = saved?.takeIf {
            it.status == WatchStatus.IN_PROGRESS && it.positionMs > 10_000 &&
                (it.durationMs == 0L || it.positionMs < it.durationMs * WATCHED_THRESHOLD)
        }?.positionMs ?: 0L
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(OnePaceLinks.pixeldrainStream(fileId).toUri(), "video/mp4")
            .putExtra("title", "${ep.number} · ${ep.title}")
            // MX Player / mpv: start at the saved minute and report the position on exit.
            .putExtra("position", resumeAt.toInt())
            .putExtra("return_result", true)
        try {
            externalLanguage = lang
            external.launch(view)
        } catch (_: ActivityNotFoundException) {
            externalLanguage = null
            Toast.makeText(this, "No encontramos otra app de video. Se usará el reproductor de la app.", Toast.LENGTH_LONG).show()
            openInternal(allowData)
        }
    }

    private fun onExternalResult(result: ActivityResult) {
        val lang = externalLanguage ?: return finish()
        val extras = result.data?.extras
        fun number(vararg keys: String): Long? = keys.firstNotNullOfOrNull { k ->
            @Suppress("DEPRECATION")
            (extras?.get(k) as? Number)?.toLong()
        }
        val report = externalResult(
            position = number("position", "extra_position"),
            duration = number("duration", "extra_duration"),
            endBy = extras?.getString("end_by"),
        )
        val c = container
        c.scope.launch {
            val ep = c.catalog.load().episode(episodeId) ?: return@launch
            c.state.update { s ->
                s.copy(progress = applyExternalResult(s.progress, ep.id, report, ep.durationMin * 60_000L, lang, System.currentTimeMillis()))
            }
        }
        finish()
    }

    companion object {
        private const val EXTRA_EPISODE = "episode"
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_QUALITY = "quality"
        private const val STATE_EXTERNAL = "external_language"

        fun intent(context: Context, episodeId: String, language: Language, quality: Quality) =
            Intent(context, PlayLauncherActivity::class.java)
                .putExtra(EXTRA_EPISODE, episodeId)
                .putExtra(EXTRA_LANGUAGE, language.name)
                .putExtra(EXTRA_QUALITY, quality.name)
    }
}
