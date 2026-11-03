package com.myonepacelist.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.myonepacelist.container
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.OnePaceLinks
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.data.progress.WatchStatus
import com.myonepacelist.domain.WATCHED_THRESHOLD
import com.myonepacelist.domain.recordPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUi(
    val episode: Episode? = null,
    val arcTitle: String = "",
    val language: Language = Language.DUB,
    val quality: Quality = Quality.Q720,
    val playing: Boolean = false,
    val buffering: Boolean = true,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    /** Set when playback resumed from a saved position, to offer "start over". */
    val resumedFromMs: Long? = null,
    val next: Episode? = null,
    /** Seconds left before the next episode starts; null when no countdown is running. */
    val countdown: Int? = null,
    val error: String? = null,
    /** Pixeldrain viewer page for the current file (browser fallback). */
    val fallbackUrl: String? = null,
)

@OptIn(UnstableApi::class)
class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent("MyOnePaceList/${com.myonepacelist.BuildConfig.VERSION_NAME} (Android)")
                    .setAllowCrossProtocolRedirects(true),
            ),
        )
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()

    private var ticker: Job? = null
    private var countdownJob: Job? = null
    private var loaded = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _ui.update { it.copy(playing = isPlaying) }
                if (!isPlaying) persist()
            }

            override fun onPlaybackStateChanged(state: Int) {
                _ui.update { it.copy(buffering = state == Player.STATE_BUFFERING, durationMs = player.duration.coerceAtLeast(0)) }
                if (state == Player.STATE_ENDED) onEnded()
            }

            override fun onPlayerError(error: PlaybackException) {
                val http = error.cause as? HttpDataSource.InvalidResponseCodeException
                val msg = when {
                    http?.responseCode == 403 || http?.responseCode == 429 ->
                        "Pixeldrain pidió una verificación o alcanzaste su límite diario gratuito."
                    http?.responseCode == 404 -> "El archivo ya no está disponible en Pixeldrain."
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Sin conexión. Revisa tu internet e inténtalo de nuevo."
                    else -> "No se pudo reproducir este episodio."
                }
                _ui.update { it.copy(error = msg, buffering = false) }
            }
        })
    }

    fun load(episodeId: String, language: Language, quality: Quality) {
        if (loaded && _ui.value.episode?.id == episodeId) return
        loaded = true
        viewModelScope.launch {
            val catalog = c.catalog.load()
            val ep = catalog.episode(episodeId) ?: run {
                _ui.update { it.copy(error = "Episodio no encontrado") }
                return@launch
            }
            val lang = ep.resolveLanguage(language) ?: run {
                _ui.update { it.copy(episode = ep, error = "Este episodio aún no está en español.", fallbackUrl = OnePaceLinks.episodePage(ep.id)) }
                return@launch
            }
            val fileId = ep.fileId(lang, quality)!!
            val saved = c.state.current().progress[ep.id]
            val resumeAt = saved?.takeIf {
                it.status == WatchStatus.IN_PROGRESS && it.positionMs > 10_000 &&
                    (it.durationMs == 0L || it.positionMs < it.durationMs * WATCHED_THRESHOLD)
            }?.positionMs
            _ui.value = PlayerUi(
                episode = ep,
                arcTitle = catalog.arc(ep.arc)?.title.orEmpty(),
                language = lang,
                quality = quality,
                resumedFromMs = resumeAt,
                next = catalog.next(ep.id),
                fallbackUrl = OnePaceLinks.pixeldrainViewer(fileId),
            )
            val item = MediaItem.Builder()
                .setUri(OnePaceLinks.pixeldrainStream(fileId))
                .setMediaId(ep.id)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(ep.title).setSubtitle(_ui.value.arcTitle).build())
                .build()
            player.setMediaItem(item, resumeAt ?: 0L)
            player.prepare()
            player.play()
            startTicker()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            var n = 0
            while (isActive) {
                _ui.update { it.copy(positionMs = player.currentPosition, durationMs = player.duration.coerceAtLeast(0)) }
                if (++n % 5 == 0 && player.isPlaying) persist()
                delay(1000)
            }
        }
    }

    /** Saves the current position (and marks the episode watched past 90%). */
    fun persist() {
        val ep = _ui.value.episode ?: return
        val pos = player.currentPosition
        val dur = player.duration
        if (dur <= 0 || pos < 5_000) return
        val lang = _ui.value.language
        c.scope.launch {
            c.state.update { s -> s.copy(progress = recordPosition(s.progress, ep.id, pos, dur, lang, System.currentTimeMillis())) }
        }
    }

    private fun onEnded() {
        persist()
        val next = _ui.value.next ?: return
        viewModelScope.launch {
            if (!c.state.current().settings.autoplayNext || !next.hasSpanish) return@launch
            countdownJob?.cancel()
            countdownJob = launch {
                for (s in 8 downTo 1) {
                    _ui.update { it.copy(countdown = s) }
                    delay(1000)
                }
                playNext()
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _ui.update { it.copy(countdown = null) }
    }

    fun playNext() {
        val next = _ui.value.next ?: return
        countdownJob?.cancel()
        persist()
        loaded = false
        val lang = _ui.value.language
        load(next.id, lang, _ui.value.quality)
    }

    fun togglePlay() = if (player.isPlaying) player.pause() else {
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }

    fun seekBy(ms: Long) = player.seekTo((player.currentPosition + ms).coerceIn(0, player.duration.coerceAtLeast(0)))
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun startOver() {
        player.seekTo(0)
        dismissResume()
    }
    fun dismissResume() = _ui.update { it.copy(resumedFromMs = null) }

    fun retry() {
        _ui.update { it.copy(error = null, buffering = true) }
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
        persist()
    }

    override fun onCleared() {
        persist()
        player.release()
    }
}
