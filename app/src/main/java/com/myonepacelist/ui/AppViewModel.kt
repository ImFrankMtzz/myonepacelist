package com.myonepacelist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myonepacelist.container
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.progress.AppState
import com.myonepacelist.data.progress.Settings
import com.myonepacelist.domain.BackupCodec
import com.myonepacelist.domain.catalogDiff
import com.myonepacelist.domain.markUpTo
import com.myonepacelist.domain.setWatched
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val catalog: Catalog? = null,
    val app: AppState = AppState(),
    val ready: Boolean = false,
) {
    val progress get() = app.progress
    val settings get() = app.settings
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val c = application.container
    private val now get() = System.currentTimeMillis()

    val ui: StateFlow<UiState> = combine(c.catalog.catalog, c.state.state) { cat, st ->
        UiState(catalog = cat, app = st, ready = cat != null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    /** One-off messages for the snackbar. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        viewModelScope.launch { c.catalog.load() }
    }

    private fun catalog() = ui.value.catalog

    fun toggleWatched(episodeId: String) = viewModelScope.launch {
        c.state.update { s ->
            val watched = s.progress[episodeId]?.watched == true
            s.copy(progress = setWatched(s.progress, episodeId, !watched, now))
        }
    }

    fun setWatched(episodeId: String, watched: Boolean) = viewModelScope.launch {
        c.state.update { s -> s.copy(progress = setWatched(s.progress, episodeId, watched, now)) }
    }

    fun markUpTo(episodeId: String) = viewModelScope.launch {
        val cat = catalog() ?: return@launch
        c.state.update { s -> s.copy(progress = markUpTo(cat, s.progress, episodeId, inclusive = true, now = now)) }
        _messages.send("Marcado como visto hasta aquí")
    }

    fun markArc(slug: String, watched: Boolean) = viewModelScope.launch {
        val arc = catalog()?.arc(slug) ?: return@launch
        c.state.update { s ->
            var p = s.progress
            arc.episodes.forEachIndexed { i, e ->
                if (!watched) p = p - e.id
                else if (p[e.id]?.watched != true) p = setWatched(p, e.id, true, now - (arc.episodes.size - i))
            }
            s.copy(progress = p)
        }
    }

    /** @param nextEpisodeId the episode the user is about to watch (everything before it is marked watched), or null to start fresh. */
    fun finishOnboarding(nextEpisodeId: String?, language: Language) = viewModelScope.launch {
        val cat = catalog()
        c.state.update { s ->
            val p = if (nextEpisodeId != null && cat != null) markUpTo(cat, s.progress, nextEpisodeId, inclusive = false, now = now) else s.progress
            s.copy(onboarded = true, progress = p, settings = s.settings.copy(language = language))
        }
    }

    fun updateSettings(transform: (Settings) -> Settings) = viewModelScope.launch {
        c.state.update { it.copy(settings = transform(it.settings)) }
    }

    fun exportBackup(): String = BackupCodec.export(ui.value.app, now)

    fun importBackup(text: String) = viewModelScope.launch {
        runCatching { BackupCodec.decode(text) }
            .onSuccess { b ->
                c.state.update { BackupCodec.merge(it, b) }
                _messages.send("Progreso importado: ${b.progress.size} episodios")
            }
            .onFailure { _messages.send(it.message ?: "No se pudo importar el archivo") }
    }

    fun resetAll() = viewModelScope.launch {
        c.state.update { AppState(settings = it.settings) }
    }

    fun refreshCatalog() = viewModelScope.launch {
        runCatching { c.catalog.refresh() }
            .onSuccess { change ->
                if (change == null) _messages.send("El catálogo ya está al día")
                else {
                    val d = catalogDiff(change.first, change.second)
                    c.state.update { it.copy(knownCatalogVersion = change.second.version) }
                    _messages.send(
                        if (d.isEmpty) "Catálogo actualizado"
                        else "Catálogo actualizado: ${d.newEpisodes.size} episodios nuevos, ${d.newDubs.size} con doblaje nuevo",
                    )
                }
            }
            .onFailure { _messages.send("Sin conexión: se usa el catálogo guardado") }
    }

    fun message(text: String) = viewModelScope.launch { _messages.send(text) }
}
