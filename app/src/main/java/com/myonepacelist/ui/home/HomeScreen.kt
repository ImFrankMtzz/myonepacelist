package com.myonepacelist.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.progress.WatchStatus
import com.myonepacelist.domain.arcProgress
import com.myonepacelist.domain.formatMinutes
import com.myonepacelist.domain.nextEpisode
import com.myonepacelist.ui.UiState
import com.myonepacelist.ui.components.EpisodeSegments
import com.myonepacelist.ui.components.ProgressBar
import com.myonepacelist.ui.components.SectionLabel
import com.myonepacelist.ui.components.SegmentState
import com.myonepacelist.ui.components.Segmented
import com.myonepacelist.ui.components.Thumb
import com.myonepacelist.ui.components.availabilityLabel
import com.myonepacelist.ui.components.formatClock
import com.myonepacelist.ui.components.label
import com.myonepacelist.ui.theme.Tokens

@Composable
fun HomeScreen(
    ui: UiState,
    contentPadding: PaddingValues,
    onSettings: () -> Unit,
    onOpenEpisode: (String) -> Unit,
    onOpenArc: (String) -> Unit,
    onPlay: (Episode, Language) -> Unit,
    onOpenWeb: (Episode) -> Unit,
    onMarkWatched: (Episode) -> Unit,
) {
    val catalog = ui.catalog ?: return
    val next = nextEpisode(catalog, ui.progress)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buildAnnotatedString {
                        append("MyOnePace")
                        withStyle(SpanStyle(color = Tokens.accent)) { append("List") }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Tokens.text,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Ajustes", tint = Tokens.textMuted)
                }
            }
        }
        if (next == null) {
            item { AllDone(Modifier.padding(horizontal = 20.dp)) }
        } else {
            item {
                ContinueCard(
                    catalog, ui, next,
                    onPlay = onPlay, onOpenWeb = onOpenWeb, onMarkWatched = onMarkWatched,
                    onOpen = { onOpenEpisode(next.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item { CurrentArc(catalog, ui, next, onOpenArc, Modifier.padding(horizontal = 20.dp)) }
            val upcoming = catalog.episodes.drop(catalog.indexOf(next.id) + 1)
                .filter { ui.progress[it.id]?.watched != true }
                .take(3)
            if (upcoming.isNotEmpty()) {
                item { SectionLabel("A continuación", Modifier.padding(horizontal = 20.dp)) }
                upcoming.forEach { ep ->
                    item(key = ep.id) {
                        UpcomingRow(catalog, ep, onClick = { onOpenEpisode(ep.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(
    catalog: Catalog,
    ui: UiState,
    ep: Episode,
    onPlay: (Episode, Language) -> Unit,
    onOpenWeb: (Episode) -> Unit,
    onMarkWatched: (Episode) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val progress = ui.progress[ep.id]
    val inProgress = progress?.status == WatchStatus.IN_PROGRESS && progress.positionMs > 0
    val arc = catalog.arc(ep.arc)
    var language by remember(ep.id, ui.settings.language) {
        mutableStateOf(ep.resolveLanguage(progress?.language ?: ui.settings.language) ?: ui.settings.language)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Tokens.surface)
            .border(1.dp, Tokens.border, RoundedCornerShape(24.dp)),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clickable(onClickLabel = "Ver detalles", onClick = onOpen)) {
            Thumb(ep.image, ep.number, Modifier.fillMaxSize(), width = 1080, contentDescription = "Escena de ${ep.title}")
            Text(
                if (inProgress) "CONTINUAR" else "SIGUIENTE",
                Modifier.padding(12.dp).clip(RoundedCornerShape(50)).background(Tokens.bg.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Tokens.text,
            )
            if (inProgress) {
                ProgressBar(
                    progress!!.fraction,
                    Modifier.align(Alignment.BottomStart),
                    height = 4.dp,
                )
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${arc?.title ?: ""} · Episodio ${ep.number}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Tokens.textMuted)
                Text(ep.title, style = MaterialTheme.typography.headlineSmall, color = Tokens.text)
                val meta = buildList {
                    add(if (inProgress) "${formatClock(progress!!.positionMs)} de ${ep.durationMin} min" else "${ep.durationMin} min")
                    if (ep.chapters.isNotBlank()) add("Cap. ${ep.chapters}")
                }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted)
            }
            if (ep.hasSpanish) {
                Segmented(
                    options = Language.entries,
                    selected = language,
                    onSelect = { language = it },
                    label = { it.label },
                    enabled = { ep.available(it) },
                    icon = { if (it == Language.DUB) Icons.Rounded.Mic else Icons.Rounded.Subtitles },
                )
                if (!ep.available(Language.DUB)) {
                    Text("Este episodio aún no tiene doblaje latino.", style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted)
                }
                Button(
                    onClick = { onPlay(ep, language) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (inProgress) "Reanudar en ${formatClock(progress!!.positionMs)}" else "Ver episodio",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Text(
                    "One Pace todavía no publica este episodio en español. Puedes verlo en onepace.net o marcarlo como visto.",
                    style = MaterialTheme.typography.bodyMedium, color = Tokens.textSoft,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOpenWeb(ep) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = Tokens.text)
                        Spacer(Modifier.width(6.dp))
                        Text("onepace.net", color = Tokens.text)
                    }
                    OutlinedButton(onClick = { onMarkWatched(ep) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Tokens.text)
                        Spacer(Modifier.width(6.dp))
                        Text("Marcar visto", color = Tokens.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentArc(catalog: Catalog, ui: UiState, next: Episode, onOpenArc: (String) -> Unit, modifier: Modifier) {
    val arc = catalog.arc(next.arc) ?: return
    val ap = arcProgress(arc, ui.progress)
    val remaining = arc.episodes.filter { ui.progress[it.id]?.watched != true }.sumOf { it.durationMin }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClickLabel = "Ver ${arc.title}") { onOpenArc(arc.slug) }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(arc.title, style = MaterialTheme.typography.titleMedium, color = Tokens.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${ap.watched} de ${ap.total}" + if (remaining > 0) " · ${formatMinutes(remaining)} restantes" else "",
                style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted,
            )
        }
        if (arc.episodes.size <= 24) {
            EpisodeSegments(
                arc.episodes.map {
                    when {
                        ui.progress[it.id]?.watched == true -> SegmentState.WATCHED
                        it.id == next.id -> SegmentState.CURRENT
                        else -> SegmentState.PENDING
                    }
                },
            )
        } else {
            ProgressBar(ap.fraction)
        }
    }
}

@Composable
private fun UpcomingRow(catalog: Catalog, ep: Episode, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Thumb(ep.image, ep.number, Modifier.width(88.dp).height(56.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${ep.number} · ${ep.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Tokens.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val arc = catalog.arc(ep.arc)?.title
            Text(
                listOfNotNull(arc.takeIf { ep.part == 1.0 }, "${ep.durationMin} min", ep.availabilityLabel()).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AllDone(modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Tokens.surface).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("¡Llegaste al final!", style = MaterialTheme.typography.headlineSmall, color = Tokens.text)
        Text(
            "Viste todo lo que One Pace ha publicado. Te avisaremos cuando salga un episodio nuevo.",
            style = MaterialTheme.typography.bodyMedium, color = Tokens.textSoft,
        )
    }
}
