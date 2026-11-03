package com.myonepacelist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.OnePaceLinks
import com.myonepacelist.ui.theme.SpaceGrotesk
import com.myonepacelist.ui.theme.Tokens

val Language.label: String
    get() = when (this) {
        Language.DUB -> "Doblaje latino"
        Language.SUB -> "Subtítulos"
    }

/** "Doblaje y subtítulos" / "Solo subtítulos" / "Sin versión en español aún" */
fun Episode.availabilityLabel(): String = when {
    dub != null && sub != null -> "Doblaje y subtítulos"
    dub != null -> "Solo doblaje"
    sub != null -> "Solo subtítulos"
    else -> "Sin versión en español aún"
}

/** iOS-style segmented control built from accessible radio buttons. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: (T) -> Boolean = { true },
    icon: ((T) -> ImageVector?)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Tokens.bg)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { opt ->
            val on = opt == selected
            val ok = enabled(opt)
            val bg by animateColorAsState(if (on) Tokens.surface2 else Color.Transparent, label = "seg")
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .then(if (on) Modifier.border(1.dp, Tokens.accent, RoundedCornerShape(10.dp)) else Modifier)
                    .selectable(selected = on, enabled = ok, role = Role.RadioButton) { onSelect(opt) }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = when {
                    !ok -> Tokens.textMuted.copy(alpha = 0.45f)
                    on -> Tokens.text
                    else -> Tokens.textMuted
                }
                icon?.invoke(opt)?.let {
                    Icon(it, null, tint = color, modifier = Modifier.size(18.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                }
                Text(
                    label(opt),
                    color = color,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp, color: Color = Tokens.accent) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Tokens.track),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(CircleShape).background(color))
    }
}

/** One segment per episode of an arc: watched, in progress or pending. */
@Composable
fun EpisodeSegments(states: List<SegmentState>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        states.forEach { s ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        when (s) {
                            SegmentState.WATCHED -> Tokens.accent
                            SegmentState.CURRENT -> Tokens.accentDim
                            SegmentState.PENDING -> Tokens.track
                        },
                    ),
            )
        }
    }
}

enum class SegmentState { WATCHED, CURRENT, PENDING }

@Composable
fun Thumb(src: String?, fallback: String, modifier: Modifier, width: Int = 640, contentDescription: String? = null) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Tokens.surface2), contentAlignment = Alignment.Center) {
        Text(fallback, color = Tokens.textMuted, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold)
        if (src != null) {
            AsyncImage(
                model = OnePaceLinks.image(src, width),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Tokens.surface2).padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Tokens.textMuted,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Tokens.textMuted,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp),
    )
}

/** Round check used in episode lists; amber ring marks the episode in progress. */
@Composable
fun WatchCheck(watched: Boolean, inProgress: Boolean) {
    val bg by animateColorAsState(if (watched) Tokens.accent else Color.Transparent, label = "check")
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (!watched) Modifier.border(2.dp, if (inProgress) Tokens.accent else Tokens.border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (watched) Icon(Icons.Rounded.Check, null, tint = Tokens.onAccent, modifier = Modifier.size(18.dp))
    }
}

/** 754000 -> "12:34", 4000000 -> "1:06:40" */
fun formatClock(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
