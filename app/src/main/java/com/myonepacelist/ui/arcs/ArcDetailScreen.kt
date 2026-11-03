package com.myonepacelist.ui.arcs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.progress.WatchStatus
import com.myonepacelist.domain.arcProgress
import com.myonepacelist.domain.formatMinutes
import com.myonepacelist.ui.UiState
import com.myonepacelist.ui.components.ProgressBar
import com.myonepacelist.ui.components.Tag
import com.myonepacelist.ui.components.Thumb
import com.myonepacelist.ui.components.WatchCheck
import com.myonepacelist.ui.components.availabilityLabel
import com.myonepacelist.ui.components.formatClock
import com.myonepacelist.ui.theme.SpaceGrotesk
import com.myonepacelist.ui.theme.Tokens

@Composable
fun ArcDetailScreen(
    ui: UiState,
    slug: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenEpisode: (String) -> Unit,
    onToggleWatched: (String) -> Unit,
    onMarkUpTo: (String) -> Unit,
    onMarkArc: (Boolean) -> Unit,
) {
    val arc = ui.catalog?.arc(slug) ?: return
    val ap = arcProgress(arc, ui.progress)
    var menu by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(230.dp)) {
                Thumb(arc.image, arc.title.take(2), Modifier.fillMaxSize(), width = 1080, contentDescription = arc.title)
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(Tokens.bg.copy(alpha = 0.82f))) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Tokens.text)
                    }
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp).clip(CircleShape).background(Tokens.bg.copy(alpha = 0.82f))) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones del arco", tint = Tokens.text)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Marcar arco como visto") }, onClick = { menu = false; onMarkArc(true) })
                            DropdownMenuItem(text = { Text("Reiniciar progreso del arco") }, onClick = { menu = false; onMarkArc(false) })
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(arc.title, style = MaterialTheme.typography.headlineLarge, color = Tokens.text)
                if (arc.description.isNotBlank()) {
                    Text(arc.description, style = MaterialTheme.typography.bodyMedium, color = Tokens.textSoft)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (arc.chapters.isNotBlank()) Tag("Cap. ${arc.chapters}")
                    if (arc.animeEpisodes.isNotBlank()) Tag("Anime ${arc.animeEpisodes}")
                    if (arc.durationMin > 0) Tag(formatMinutes(arc.durationMin))
                    if (arc.unfinished) Tag("En producción")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    ProgressBar(ap.fraction, Modifier.weight(1f))
                    Text("${ap.watched} de ${ap.total}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Tokens.text)
                }
                if (arc.unreleased > 0) {
                    Text(if (arc.unreleased == 1) "1 episodio más en camino." else "${arc.unreleased} episodios más en camino.", style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted)
                }
            }
        }
        items(arc.episodes, key = { it.id }) { ep ->
            EpisodeRow(
                ep, ui,
                onOpen = { onOpenEpisode(ep.id) },
                onToggle = { onToggleWatched(ep.id) },
                onMarkUpTo = { onMarkUpTo(ep.id) },
            )
        }
    }
}

@Composable
private fun EpisodeRow(ep: Episode, ui: UiState, onOpen: () -> Unit, onToggle: () -> Unit, onMarkUpTo: () -> Unit) {
    val p = ui.progress[ep.id]
    val watched = p?.watched == true
    val inProgress = p?.status == WatchStatus.IN_PROGRESS
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (inProgress) Modifier.background(Tokens.surface) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .toggleable(value = watched, role = Role.Checkbox, onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }),
            contentAlignment = Alignment.Center,
        ) { WatchCheck(watched, inProgress) }
        Column(
            Modifier.weight(1f).clickable(onClickLabel = "Ver episodio", onClick = onOpen).padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ep.number, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, color = Tokens.textMuted, style = MaterialTheme.typography.bodyMedium)
                Text(ep.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Tokens.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val meta = listOfNotNull(
                if (inProgress && p!!.positionMs > 0) "Quedaste en ${formatClock(p.positionMs)}" else null,
                "${ep.durationMin} min",
                ep.availabilityLabel(),
            ).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (inProgress) ProgressBar(p!!.fraction, Modifier.width(140.dp).padding(top = 2.dp), height = 3.dp)
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones de ${ep.number}", tint = Tokens.textMuted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (watched) "Marcar como no visto" else "Marcar como visto") }, onClick = { menu = false; onToggle() })
                DropdownMenuItem(text = { Text("Marcar visto hasta aquí") }, onClick = { menu = false; onMarkUpTo() })
            }
        }
    }
}
