package com.myonepacelist.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.data.progress.ThemeMode
import com.myonepacelist.ui.openInBrowser
import com.myonepacelist.ui.theme.MyOnePaceTheme

class PlayerActivity : ComponentActivity() {
    private val vm: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val episodeId = intent.getStringExtra(EXTRA_EPISODE) ?: return finish()
        val language = runCatching { Language.valueOf(intent.getStringExtra(EXTRA_LANGUAGE)!!) }.getOrDefault(Language.DUB)
        val quality = runCatching { Quality.valueOf(intent.getStringExtra(EXTRA_QUALITY)!!) }.getOrDefault(Quality.Q720)
        vm.load(episodeId, language, quality)
        setContent {
            MyOnePaceTheme(ThemeMode.DARK) {
                PlayerScreen(
                    vm,
                    onClose = { finish() },
                    onOpenBrowser = { url -> vm.pause(); openInBrowser(url) },
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.pause()
    }

    companion object {
        private const val EXTRA_EPISODE = "episode"
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_QUALITY = "quality"

        fun intent(context: Context, episodeId: String, language: Language, quality: Quality) =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_EPISODE, episodeId)
                .putExtra(EXTRA_LANGUAGE, language.name)
                .putExtra(EXTRA_QUALITY, quality.name)
    }
}
