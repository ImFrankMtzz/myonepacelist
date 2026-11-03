package com.myonepacelist.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.ui.components.Segmented
import com.myonepacelist.ui.components.Thumb
import com.myonepacelist.ui.components.availabilityLabel
import com.myonepacelist.ui.components.episodesLabel
import com.myonepacelist.ui.components.label
import com.myonepacelist.ui.theme.SpaceGrotesk
import com.myonepacelist.ui.theme.Tokens

private enum class Step { WELCOME, ARC, EPISODE, LANGUAGE }

/** First run: "¿Dónde te quedaste?" → arc → episode → preferred language. */
@Composable
fun OnboardingScreen(catalog: Catalog, onRestore: () -> Unit, onDone: (nextEpisodeId: String?, Language) -> Unit) {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var arcSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var episodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var language by rememberSaveable { mutableStateOf(Language.DUB) }

    BackHandler(enabled = step != Step.WELCOME) {
        step = when (step) {
            Step.LANGUAGE -> if (episodeId == null) Step.WELCOME else Step.EPISODE
            Step.EPISODE -> Step.ARC
            else -> Step.WELCOME
        }
    }

    Column(Modifier.fillMaxSize().background(Tokens.bg).safeDrawingPadding()) {
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding", modifier = Modifier.weight(1f)) { s ->
            when (s) {
                Step.WELCOME -> Welcome(
                    onFresh = { episodeId = null; step = Step.LANGUAGE },
                    onResume = { step = Step.ARC },
                    onRestore = onRestore,
                )
                Step.ARC -> Picker(
                    title = "¿En qué arco vas?",
                    onBack = { step = Step.WELCOME },
                ) {
                    items(catalog.arcs.filter { it.episodes.isNotEmpty() }, key = { it.slug }) { arc ->
                        PickRow(arc.image, arc.title, episodesLabel(arc.episodes.size)) { arcSlug = arc.slug; step = Step.EPISODE }
                    }
                }
                Step.EPISODE -> {
                    val arc = catalog.arc(arcSlug ?: "")
                    Picker(title = "¿Qué episodio vas a ver ahora?", subtitle = "Marcaremos como vistos todos los anteriores.", onBack = { step = Step.ARC }) {
                        items(arc?.episodes.orEmpty(), key = { it.id }) { ep ->
                            PickRow(ep.image, "${ep.number} · ${ep.title}", ep.availabilityLabel()) { episodeId = ep.id; step = Step.LANGUAGE }
                        }
                    }
                }
                Step.LANGUAGE -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { step = if (episodeId == null) Step.WELCOME else Step.EPISODE }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Tokens.text)
                    }
                    Text("¿Cómo prefieres verlo?", style = MaterialTheme.typography.headlineLarge, color = Tokens.text)
                    Text(
                        "Puedes cambiarlo en cada episodio. Si un episodio no tiene doblaje, usaremos los subtítulos.",
                        style = MaterialTheme.typography.bodyMedium, color = Tokens.textSoft,
                    )
                    Segmented(
                        Language.entries, language, { language = it }, { it.label },
                        icon = { if (it == Language.DUB) Icons.Rounded.Mic else Icons.Rounded.Subtitles },
                        track = Tokens.surface2,
                        selectedColor = Tokens.surface,
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { onDone(episodeId, language) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent),
                    ) { Text("Zarpar", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onFresh: () -> Unit, onResume: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        com.myonepacelist.ui.components.AppLogo(Modifier.size(120.dp).align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
        Text(
            buildAnnotatedString {
                append("MyOnePace")
                withStyle(SpanStyle(color = Tokens.accent)) { append("List") }
            },
            style = MaterialTheme.typography.displayMedium, color = Tokens.text,
        )
        Text(
            "Lleva tu avance de One Pace, la versión sin relleno de One Piece, y míralo con doblaje latino o subtítulos.",
            style = MaterialTheme.typography.bodyLarge, color = Tokens.textSoft,
        )
        Text("Sin cuentas: todo se guarda en tu teléfono.", style = MaterialTheme.typography.bodyMedium, color = Tokens.textMuted)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent),
        ) { Text("Ya voy en algún arco", style = MaterialTheme.typography.labelLarge) }
        OutlinedButton(onClick = onFresh, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Empiezo desde cero", style = MaterialTheme.typography.labelLarge, color = Tokens.text)
        }
        androidx.compose.material3.TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Restaurar un respaldo", style = MaterialTheme.typography.labelMedium, color = Tokens.accent)
        }
    }
}

@Composable
private fun Picker(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Column(Modifier.padding(start = 4.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Tokens.text)
                }
                Text(title, style = MaterialTheme.typography.headlineLarge, color = Tokens.text)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Tokens.textMuted)
            }
        }
        content()
    }
}

@Composable
private fun PickRow(image: String?, title: String, meta: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Tokens.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Thumb(image, title.take(2), Modifier.width(80.dp).height(52.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, color = Tokens.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted)
        }
    }
}
