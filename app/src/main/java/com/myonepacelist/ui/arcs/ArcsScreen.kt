package com.myonepacelist.ui.arcs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myonepacelist.data.catalog.Arc
import com.myonepacelist.domain.arcProgress
import com.myonepacelist.domain.nextEpisode
import com.myonepacelist.ui.UiState
import com.myonepacelist.ui.components.ProgressBar
import com.myonepacelist.ui.components.Thumb
import com.myonepacelist.ui.theme.Tokens

private enum class ArcFilter(val label: String) { ALL("Todos"), PENDING("Pendientes"), DUB("Con doblaje") }

@Composable
fun ArcsScreen(ui: UiState, contentPadding: PaddingValues, onOpenArc: (String) -> Unit) {
    val catalog = ui.catalog ?: return
    var filter by rememberSaveable { mutableStateOf(ArcFilter.ALL) }
    val currentArc = nextEpisode(catalog, ui.progress)?.arc
    val arcs = catalog.arcs.filter { arc ->
        when (filter) {
            ArcFilter.ALL -> true
            ArcFilter.PENDING -> !arcProgress(arc, ui.progress).complete
            ArcFilter.DUB -> arc.hasDub
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp, end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Arcos", style = MaterialTheme.typography.displaySmall, color = Tokens.text, modifier = Modifier.weight(1f))
                    Text("${catalog.arcs.size} arcos · ${catalog.episodes.size} episodios", style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArcFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Tokens.accentContainer,
                                selectedLabelColor = Tokens.accent,
                                labelColor = Tokens.textSoft,
                                containerColor = Color.Transparent,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filter == f,
                                borderColor = Tokens.border, selectedBorderColor = Tokens.accent,
                            ),
                        )
                    }
                }
            }
        }
        if (arcs.isEmpty()) {
            item {
                Text(
                    "No hay arcos con este filtro.",
                    style = MaterialTheme.typography.bodyMedium, color = Tokens.textMuted,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(arcs, key = { it.slug }) { arc ->
            ArcRow(arc, ui, current = arc.slug == currentArc, onClick = { onOpenArc(arc.slug) })
        }
    }
}


@Composable
private fun ArcRow(arc: Arc, ui: UiState, current: Boolean, onClick: () -> Unit) {
    val ap = arcProgress(arc, ui.progress)
    val dubCount = arc.episodes.count { it.dub != null }
    val meta = when {
        current -> "En curso"
        ap.complete -> "Completado"
        arc.chapters.isNotBlank() -> "Cap. ${arc.chapters}"
        else -> "${arc.episodes.size} episodios"
    } + " · " + when {
        arc.episodes.isEmpty() -> "Próximamente"
        dubCount == arc.episodes.size -> "Doblaje latino"
        dubCount > 0 -> "Doblaje en $dubCount de ${arc.episodes.size}"
        arc.episodes.any { it.sub != null } -> "Solo subtítulos"
        else -> "Sin español aún"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (current) Modifier.background(Tokens.surface).border(1.dp, Tokens.accentDim, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Thumb(arc.image, arc.title.take(2), Modifier.width(92.dp).height(69.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(arc.title, style = MaterialTheme.typography.titleSmall, color = Tokens.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${ap.watched}/${ap.total}", style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted)
            }
            ProgressBar(ap.fraction, height = 4.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ap.complete) Icon(Icons.Rounded.Check, null, tint = Tokens.accent, modifier = Modifier.size(14.dp))
                Text(meta, style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
