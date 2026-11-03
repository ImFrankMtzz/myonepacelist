package com.myonepacelist.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.data.progress.WatchStatus
import com.myonepacelist.ui.UiState
import com.myonepacelist.ui.components.SectionLabel
import com.myonepacelist.ui.components.Segmented
import com.myonepacelist.ui.components.Tag
import com.myonepacelist.ui.components.Thumb
import com.myonepacelist.ui.components.formatClock
import com.myonepacelist.ui.components.label
import com.myonepacelist.ui.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeSheet(
    ui: UiState,
    episodeId: String,
    onDismiss: () -> Unit,
    onPlay: (Episode, Language, Quality) -> Unit,
    onOpenWeb: (Episode, Language?, Quality) -> Unit,
    onToggleWatched: (Episode) -> Unit,
) {
    val catalog = ui.catalog ?: return
    val ep = catalog.episode(episodeId) ?: return
    val arc = catalog.arc(ep.arc)
    val progress = ui.progress[ep.id]
    val watched = progress?.watched == true
    val resumeAt = progress?.takeIf { it.status == WatchStatus.IN_PROGRESS && it.positionMs > 0 }?.positionMs
    var language by remember(ep.id) { mutableStateOf(ep.resolveLanguage(progress?.language ?: ui.settings.language)) }
    var quality by remember(ep.id) { mutableStateOf(ui.settings.quality) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tokens.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Thumb(ep.image, ep.number, Modifier.fillMaxWidth().aspectRatio(16f / 9f), width = 1080, contentDescription = "Escena de ${ep.title}")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${arc?.title ?: ""} · Episodio ${ep.number}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Tokens.textMuted)
                Text(ep.title, style = MaterialTheme.typography.headlineMedium, color = Tokens.text)
                if (ep.description.isNotBlank()) Text(ep.description, style = MaterialTheme.typography.bodyMedium, color = Tokens.textSoft)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (ep.chapters.isNotBlank()) Tag("Cap. ${ep.chapters}")
                    if (ep.animeEpisodes.isNotBlank()) Tag("Anime ${ep.animeEpisodes}")
                    Tag("${ep.durationMin} min")
                    if (ep.toBeRedone) Tag("Será reeditado")
                }
            }

            val lang = language
            if (lang != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Idioma")
                    Segmented(
                        Language.entries, lang, { language = it }, { it.label },
                        enabled = { ep.available(it) },
                        icon = { if (it == Language.DUB) Icons.Rounded.Mic else Icons.Rounded.Subtitles },
                    )
                    if (!ep.available(Language.DUB)) Text("Aún sin doblaje latino.", style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Calidad")
                    val available = ep.files(lang)?.keys.orEmpty()
                    Segmented(
                        Quality.entries, ep.resolveQuality(lang, quality), { quality = it }, { "${it.key}p" },
                        enabled = { it.key in available },
                    )
                }
                Button(
                    onClick = { onPlay(ep, lang, ep.resolveQuality(lang, quality)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (resumeAt != null) "Reanudar en ${formatClock(resumeAt)}" else "Reproducir", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    "One Pace todavía no publica este episodio en español.",
                    style = MaterialTheme.typography.bodyMedium, color = Tokens.textSoft,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onToggleWatched(ep) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(if (watched) Icons.Rounded.Close else Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Tokens.text)
                    Spacer(Modifier.width(6.dp))
                    Text(if (watched) "Quitar visto" else "Marcar visto", color = Tokens.text)
                }
                OutlinedButton(
                    onClick = { onOpenWeb(ep, language, quality) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = Tokens.text)
                    Spacer(Modifier.width(6.dp))
                    Text("Navegador", color = Tokens.text)
                }
            }
        }
    }
}
