package com.myonepacelist.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myonepacelist.domain.stats
import com.myonepacelist.ui.UiState
import com.myonepacelist.ui.components.ProgressBar
import com.myonepacelist.ui.components.SectionLabel
import com.myonepacelist.ui.theme.SpaceGrotesk
import com.myonepacelist.ui.theme.Tokens

private fun hours(min: Int): String = when {
    min < 60 -> "$min min"
    min < 600 && min % 60 != 0 -> "${min / 60} h ${min % 60}"
    else -> "${min / 60} h"
}

@Composable
fun StatsScreen(ui: UiState, contentPadding: PaddingValues) {
    val catalog = ui.catalog ?: return
    val s = stats(catalog, ui.progress)
    val pct = (s.fraction * 100).toInt()
    // Only arcs with any activity, plus the next pending one, keep the list short and meaningful.
    val shown = s.arcs.filter { it.watched > 0 }.let { touched ->
        val nextPending = s.arcs.firstOrNull { !it.complete && it.watched == 0 && it.total > 0 }
        (touched + listOfNotNull(nextPending)).distinctBy { it.arc.slug }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp, end = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Text("Tu viaje", style = MaterialTheme.typography.displaySmall, color = Tokens.text, modifier = Modifier.padding(horizontal = 4.dp)) }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Tokens.surface)
                    .border(1.dp, Tokens.border, RoundedCornerShape(24.dp)).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Ring(s.fraction, "$pct%", Modifier.size(120.dp).semantics { contentDescription = "$pct por ciento de One Pace visto" })
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        buildAnnotatedString {
                            append("${s.watched}")
                            withStyle(SpanStyle(fontSize = 18.sp, color = Tokens.textMuted, fontWeight = FontWeight.Medium)) { append(" / ${s.total}") }
                        },
                        style = MaterialTheme.typography.displayMedium, color = Tokens.text,
                    )
                    Text("episodios vistos", style = MaterialTheme.typography.bodyMedium, color = Tokens.textMuted)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(hours(s.watchedMinutes), "vistas", Modifier.weight(1f))
                StatTile(hours(s.remainingMinutes), "por delante", Modifier.weight(1f))
                StatTile("${s.arcsCompleted} / ${catalog.arcs.size}", "arcos completos", Modifier.weight(1f))
            }
        }
        item { SectionLabel("Por arco", Modifier.padding(horizontal = 4.dp)) }
        items(shown, key = { it.arc.slug }) { a ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(a.arc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Tokens.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.watched} / ${a.total}", style = MaterialTheme.typography.bodyMedium, color = Tokens.textMuted)
                }
                ProgressBar(a.fraction, color = if (a.complete) Tokens.accent else Tokens.accentDim)
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Tokens.surface).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Tokens.text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted)
    }
}

@Composable
private fun Ring(fraction: Float, label: String, modifier: Modifier) {
    val f by animateFloatAsState(fraction, label = "ring")
    val track = Tokens.track
    val accent = Tokens.accent
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, useCenter = false, style = stroke)
            if (f > 0f) drawArc(accent, -90f, 360f * f.coerceAtLeast(0.01f), useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Tokens.text)
            Text("completado", style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted)
        }
    }
}
