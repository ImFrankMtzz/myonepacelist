package com.myonepacelist.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.myonepacelist.ui.components.formatClock
import com.myonepacelist.ui.components.label
import com.myonepacelist.ui.theme.SpaceGrotesk
import com.myonepacelist.ui.theme.Tokens
import kotlinx.coroutines.delay

private val Scrim = Color(0x8C000000)
private val OnVideo = Color(0xFFF2F4F7)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(vm: PlayerViewModel, onClose: () -> Unit, onOpenBrowser: (String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var controls by remember { mutableStateOf(true) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    var tapToken by remember { mutableStateOf(0) }

    // Auto-hide the controls 3 s after the last interaction while playing.
    LaunchedEffect(controls, ui.playing, tapToken, scrubbing) {
        if (controls && ui.playing && scrubbing == null && ui.error == null) {
            delay(3000)
            controls = false
        }
    }
    LaunchedEffect(ui.resumedFromMs) {
        if (ui.resumedFromMs != null) {
            delay(6000)
            vm.dismissResume()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controls = !controls; tapToken++ },
                    onDoubleTap = { o ->
                        vm.seekBy(if (o.x < size.width / 2) -10_000 else 10_000)
                        tapToken++
                    },
                )
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    player = vm.player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (ui.buffering && ui.error == null) {
            CircularProgressIndicator(color = Tokens.accent, modifier = Modifier.align(Alignment.Center).size(56.dp))
        }

        AnimatedVisibility(controls && ui.error == null && ui.countdown == null && !ui.pausedForData, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color(0x59000000)).safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth().align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Salir del reproductor", onClose)
                    Column(Modifier.weight(1f)) {
                        Text(ui.episode?.title.orEmpty(), fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, color = OnVideo, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text("${ui.arcTitle} · Episodio ${ui.episode?.number.orEmpty()}", color = Color(0xFFC4CBD5), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "${ui.language.label} · ${ui.quality.key}p",
                        Modifier.clip(RoundedCornerShape(50)).background(Scrim).padding(horizontal = 12.dp, vertical = 8.dp),
                        color = OnVideo, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIcon(Icons.Rounded.Replay10, "Retroceder 10 segundos", { vm.seekBy(-10_000); tapToken++ }, size = 56)
                    IconButton(
                        onClick = { vm.togglePlay(); tapToken++ },
                        modifier = Modifier.size(76.dp).clip(CircleShape).background(Tokens.accent),
                    ) {
                        Icon(if (ui.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (ui.playing) "Pausar" else "Reproducir", tint = Tokens.onAccent, modifier = Modifier.size(38.dp))
                    }
                    RoundIcon(Icons.Rounded.Forward10, "Adelantar 10 segundos", { vm.seekBy(10_000); tapToken++ }, size = 56)
                }

                Column(Modifier.fillMaxWidth().align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dur = ui.durationMs.coerceAtLeast(1)
                    val shown = scrubbing ?: (ui.positionMs.toFloat() / dur)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(formatClock((shown * dur).toLong()), color = OnVideo, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = shown.coerceIn(0f, 1f),
                            onValueChange = { scrubbing = it },
                            onValueChangeFinished = { scrubbing?.let { vm.seekTo((it * dur).toLong()) }; scrubbing = null },
                            colors = SliderDefaults.colors(thumbColor = Tokens.accent, activeTrackColor = Tokens.accent, inactiveTrackColor = Color(0x47FFFFFF)),
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatClock(ui.durationMs), color = OnVideo, style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tu avance se guarda solo", color = Color(0xFFC4CBD5), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        ui.next?.let { next ->
                            TextButton(onClick = { vm.playNext() }, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Scrim)) {
                                Text("Siguiente: ${next.number} · ${next.title}", color = OnVideo, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 280.dp))
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Rounded.SkipNext, null, tint = OnVideo, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // "Reanudando en 12:34 · Desde el inicio"
        AnimatedVisibility(ui.resumedFromMs != null && ui.error == null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = 80.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Scrim).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Reanudando en ${formatClock(ui.resumedFromMs ?: 0)}", color = OnVideo, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { vm.startOver() }) { Text("Desde el inicio", color = Tokens.accent, fontWeight = FontWeight.SemiBold) }
            }
        }

        ui.countdown?.let { s ->
            Overlay {
                Text("Siguiente episodio en $s", color = OnVideo, style = MaterialTheme.typography.titleLarge)
                Text("${ui.next?.number} · ${ui.next?.title}", color = Color(0xFFC4CBD5), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { vm.cancelCountdown() }) { Text("Cancelar") }
                    Button(onClick = { vm.playNext() }, colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent)) { Text("Ver ahora") }
                }
            }
        }

        if (ui.pausedForData && ui.error == null) {
            Overlay {
                Text("Pausamos: estás con datos móviles", color = OnVideo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Pediste no reproducir con datos. Vuelve al Wi-Fi o sigue con datos solo esta vez.",
                    color = Color(0xFFC4CBD5), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(max = 460.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onClose) { Text("Salir") }
                    Button(onClick = { vm.continueOnData() }, colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent)) {
                        Text("Seguir con datos")
                    }
                }
            }
        }

        ui.error?.let { msg ->
            Overlay {
                Text(msg, color = OnVideo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(max = 460.dp))
                Text("Puedes abrirlo en el navegador; tu avance de este episodio queda como está.", color = Color(0xFFC4CBD5), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(max = 460.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onClose) { Text("Volver") }
                    if (ui.episode?.hasSpanish == true) {
                        FilledTonalButton(onClick = { vm.retry() }) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Reintentar")
                        }
                    }
                    ui.fallbackUrl?.let { url ->
                        Button(onClick = { onOpenBrowser(url) }, colors = ButtonDefaults.buttonColors(containerColor = Tokens.accent, contentColor = Tokens.onAccent)) {
                            Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Abrir en el navegador")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xD9000000)).safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit, size: Int = 48) {
    IconButton(onClick = onClick, modifier = Modifier.size(size.dp).heightIn(min = 48.dp).clip(CircleShape).background(Scrim)) {
        Icon(icon, contentDescription = description, tint = OnVideo, modifier = Modifier.size((size * 0.5).dp))
    }
}
