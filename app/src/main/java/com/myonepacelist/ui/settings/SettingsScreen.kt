package com.myonepacelist.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.myonepacelist.BuildConfig
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.data.progress.Settings
import com.myonepacelist.data.progress.ThemeMode
import com.myonepacelist.ui.AppViewModel
import com.myonepacelist.ui.UiState
import com.myonepacelist.ui.components.SectionLabel
import com.myonepacelist.ui.components.Segmented
import com.myonepacelist.ui.components.label
import com.myonepacelist.ui.openInBrowser
import com.myonepacelist.ui.theme.Tokens
import java.time.LocalDate

@Composable
fun SettingsScreen(ui: UiState, vm: AppViewModel, contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = ui.settings
    var confirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(vm.exportBackup().encodeToByteArray()) }
        }.onSuccess { vm.message("Progreso exportado") }.onFailure { vm.message("No se pudo guardar el archivo") }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
            .onSuccess { vm.importBackup(it) }
            .onFailure { vm.message("No se pudo leer el archivo") }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.updateSettings { it.copy(notifications = granted) }
        if (!granted) vm.message("Sin permiso de notificaciones no podemos avisarte")
    }
    fun setNotifications(on: Boolean) {
        val needsPermission = on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.updateSettings { it.copy(notifications = on) }
    }
    fun update(t: (Settings) -> Settings) = vm.updateSettings(t)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
            start = 20.dp, end = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Tokens.text)
                }
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = Tokens.text)
            }
        }

        item { Group("Reproducción") {
            Label("Idioma preferido")
            Segmented(Language.entries, s.language, { l -> update { it.copy(language = l) } }, { it.label })
            Label("Calidad preferida")
            Segmented(Quality.entries, s.quality, { q -> update { it.copy(quality = q) } }, { "${it.key}p" })
            Text("720p equilibra calidad y datos móviles.", style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted)
            SwitchRow("Reproducir el siguiente episodio al terminar", s.autoplayNext) { v -> update { it.copy(autoplayNext = v) } }
        } }

        item { Group("Apariencia") {
            Segmented(
                ThemeMode.entries, s.theme, { t -> update { it.copy(theme = t) } },
                { when (it) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.DARK -> "Oscuro"; ThemeMode.LIGHT -> "Claro" } },
            )
        } }

        item { Group("Avisos") {
            SwitchRow("Avisarme cuando haya episodios nuevos o doblaje nuevo", s.notifications, ::setNotifications)
        } }

        item { Group("Tu progreso") {
            Text(
                "Tu progreso solo vive en este teléfono. Exporta un respaldo para no perderlo al cambiar de equipo.",
                style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted,
            )
            ActionRow(Icons.Rounded.Upload, "Exportar progreso") { exportLauncher.launch("myonepacelist-${LocalDate.now()}.json") }
            ActionRow(Icons.Rounded.Download, "Importar progreso") { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            ActionRow(Icons.Rounded.RestartAlt, "Borrar todo el progreso", danger = true) { confirmReset = true }
        } }

        item { Group("Catálogo") {
            val date = ui.catalog?.generatedAt?.take(10).orEmpty()
            Text(
                "${ui.catalog?.episodes?.size ?: 0} episodios · actualizado el $date",
                style = MaterialTheme.typography.bodySmall, color = Tokens.textMuted,
            )
            ActionRow(Icons.Rounded.Refresh, "Buscar episodios nuevos") { vm.refreshCatalog() }
        } }

        item { Group("Acerca de") {
            Text(
                "MyOnePaceList es un proyecto de fans, sin afiliación con One Pace, Toei Animation ni Shueisha. " +
                    "No aloja videos: reproduce los archivos públicos que One Pace enlaza en onepace.net. " +
                    "No usa cuentas, anuncios ni rastreo.",
                style = MaterialTheme.typography.bodySmall, color = Tokens.textSoft,
            )
            ActionRow(null, "Visitar onepace.net") { context.openInBrowser("https://onepace.net/es-419") }
            Text(
                "Versión ${BuildConfig.VERSION_NAME} · Tipografías Inter y Space Grotesk (SIL Open Font License)",
                style = MaterialTheme.typography.labelSmall, color = Tokens.textMuted,
            )
        } }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Borrar todo el progreso?") },
            text = { Text("Se perderán los episodios vistos y los minutos guardados. Si no tienes un respaldo, no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; vm.resetAll() }) { Text("Borrar", color = Tokens.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
            containerColor = Tokens.surface,
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tokens.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Tokens.text, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Tokens.text, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Tokens.accent, checkedThumbColor = Tokens.onAccent),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector?, text: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val color = if (danger) Tokens.error else Tokens.text
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(22.dp).clip(CircleShape))
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
    }
}
