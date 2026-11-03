package com.myonepacelist.widget

import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.myonepacelist.data.catalog.OnePaceLinks
import androidx.glance.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.myonepacelist.MainActivity
import com.myonepacelist.R
import com.myonepacelist.container
import com.myonepacelist.domain.arcProgress
import com.myonepacelist.domain.nextEpisode
import com.myonepacelist.player.PlayerActivity

private val Bg = ColorProvider(Color(0xFF161B22))
private val Text1 = ColorProvider(Color(0xFFE8ECF1))
private val Muted = ColorProvider(Color(0xFF9AA4B2))
private val Soft = ColorProvider(Color(0xFFD5DAE1))
private val Accent = ColorProvider(Color(0xFFF4B740))
private val Track = ColorProvider(Color(0xFF262D3A))

/** Home-screen widget: the next episode, one tap away. */
class NextEpisodeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 120.dp), DpSize(260.dp, 120.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val c = context.container
        val initialCatalog = c.catalog.load()
        val initialState = c.state.current()
        // Read state inside the composition: code before provideContent only runs when the
        // widget session starts, so anything computed there would go stale on later updates.
        provideContent {
            val catalog = c.catalog.catalog.collectAsState().value ?: initialCatalog
            val state = c.state.state.collectAsState(initialState).value
            val next = if (state.onboarded) nextEpisode(catalog, state.progress) else null
            val arc = next?.let { catalog.arc(it.arc) }
            val ap = arc?.let { arcProgress(it, state.progress) }
            val lang = next?.resolveLanguage(state.progress[next.id]?.language ?: state.settings.language)
            val intent = when {
                next == null -> context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                lang == null -> MainActivity.episodeIntent(context, next.id)
                else -> PlayerActivity.intent(context, next.id, lang, state.settings.quality)
            }
            val backdrop = produceState<Bitmap?>(null, arc?.image) { value = loadBackdrop(context, arc?.image) }.value
            Content(
                backdrop = backdrop,
                title = next?.title ?: if (state.onboarded) "¡Viste todo!" else "MyOnePaceList",
                subtitle = when {
                    next != null -> "${arc?.title} · ${next.number}"
                    state.onboarded -> "Te avisaremos del próximo episodio"
                    else -> "Toca para empezar"
                },
                fraction = ap?.fraction ?: 0f,
                progressLabel = ap?.let { "${it.watched}/${it.total}" },
                onClick = actionStartActivity(intent),
            )
        }
    }

    /** The current arc's backdrop as a software bitmap (RemoteViews can't take hardware ones). Cached by Coil. */
    private suspend fun loadBackdrop(context: Context, src: String?): Bitmap? {
        val url = OnePaceLinks.image(src, 640) ?: return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(640, 400)
            .allowHardware(false)
            .build()
        return (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
    }

    @Composable
    private fun Content(
        backdrop: Bitmap?,
        title: String,
        subtitle: String,
        fraction: Float,
        progressLabel: String?,
        onClick: androidx.glance.action.Action,
    ) {
        val wide = LocalSize.current.width >= 250.dp
        Box(GlanceModifier.fillMaxSize().background(Bg).cornerRadius(24.dp).clickable(onClick)) {
            if (backdrop != null) {
                Image(ImageProvider(backdrop), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Image(ImageProvider(R.drawable.widget_scrim), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
            Body(title, subtitle, fraction, progressLabel, wide, onImage = backdrop != null)
        }
    }

    @Composable
    private fun Body(title: String, subtitle: String, fraction: Float, progressLabel: String?, wide: Boolean, onImage: Boolean) {
        val meta = if (onImage) Soft else Muted
        Column(GlanceModifier.fillMaxSize().padding(16.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SIGUIENTE", style = TextStyle(color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                if (progressLabel != null) Text(progressLabel, style = TextStyle(color = meta, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(title, maxLines = 2, style = TextStyle(color = Text1, fontSize = if (wide) 17.sp else 15.sp, fontWeight = FontWeight.Bold))
                    Text(subtitle, maxLines = 1, style = TextStyle(color = meta, fontSize = 12.sp))
                }
                if (wide) {
                    Spacer(GlanceModifier.width(12.dp))
                    Box(GlanceModifier.size(48.dp).background(Accent).cornerRadius(24.dp), contentAlignment = Alignment.Center) {
                        Image(ImageProvider(R.drawable.ic_widget_play), contentDescription = "Reproducir", modifier = GlanceModifier.size(24.dp))
                    }
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = fraction,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                color = Accent,
                backgroundColor = Track,
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) = runCatching { NextEpisodeWidget().updateAll(context) }
    }
}

class NextEpisodeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextEpisodeWidget()
}
