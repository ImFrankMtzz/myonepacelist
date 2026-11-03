package com.myonepacelist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.myonepacelist.data.catalog.Episode
import com.myonepacelist.data.catalog.Language
import com.myonepacelist.data.catalog.OnePaceLinks
import com.myonepacelist.data.catalog.Quality
import com.myonepacelist.ui.arcs.ArcDetailScreen
import com.myonepacelist.ui.arcs.ArcsScreen
import com.myonepacelist.ui.episode.EpisodeSheet
import com.myonepacelist.ui.home.HomeScreen
import com.myonepacelist.ui.onboarding.OnboardingScreen
import com.myonepacelist.ui.settings.SettingsScreen
import com.myonepacelist.ui.stats.StatsScreen
import com.myonepacelist.ui.theme.MyOnePaceTheme
import com.myonepacelist.ui.theme.Tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ArcsRoute
@Serializable object StatsRoute
@Serializable object SettingsRoute
@Serializable data class ArcRoute(val slug: String)

private data class Tab(val route: Any, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Inicio", Icons.Rounded.Home),
    Tab(ArcsRoute, "Arcos", Icons.AutoMirrored.Rounded.FormatListBulleted),
    Tab(StatsRoute, "Estadísticas", Icons.Rounded.BarChart),
)

/** @param openEpisode episode ids to show (deep links from the widget and notifications). */
@Composable
fun MainScreen(vm: AppViewModel, openEpisode: Flow<String>) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    MyOnePaceTheme(ui.settings.theme) {
        val catalog = ui.catalog
        when {
            !ui.ready || catalog == null -> Box(Modifier.fillMaxSize())
            !ui.app.onboarded -> OnboardingScreen(catalog) { next, lang -> vm.finishOnboarding(next, lang) }
            else -> AppScaffold(ui, vm, openEpisode)
        }
    }
}

@Composable
private fun AppScaffold(ui: UiState, vm: AppViewModel, openEpisode: Flow<String>) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var sheetEpisode by rememberSaveable { mutableStateOf<String?>(null) }
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar = tabs.any { tab -> destination?.hierarchy().orEmpty().any { it.hasRoute(tab.route::class) } }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { openEpisode.collect { sheetEpisode = it } }

    val play: (Episode, Language, Quality) -> Unit = { ep, lang, q ->
        sheetEpisode = null
        context.playEpisode(ep.id, lang, q)
    }
    val openWeb: (Episode, Language?) -> Unit = { ep, lang ->
        val file = lang?.let { ep.fileId(it, ui.settings.quality) }
        context.openInBrowser(if (file != null) OnePaceLinks.pixeldrainViewer(file) else OnePaceLinks.episodePage(ep.id))
    }

    Scaffold(
        containerColor = Tokens.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Tokens.nav) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy().orEmpty().any { it.hasRoute(tab.route::class) }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Tokens.accent,
                                selectedTextColor = Tokens.text,
                                indicatorColor = Tokens.accentContainer,
                                unselectedIconColor = Tokens.textMuted,
                                unselectedTextColor = Tokens.textMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    ui, padding,
                    onSettings = { nav.navigate(SettingsRoute) },
                    onOpenEpisode = { sheetEpisode = it },
                    onOpenArc = { nav.navigate(ArcRoute(it)) },
                    onPlay = { ep, lang -> play(ep, lang, ui.settings.quality) },
                    onOpenWeb = { openWeb(it, null) },
                    onMarkWatched = { vm.setWatched(it.id, true) },
                )
            }
            composable<ArcsRoute> { ArcsScreen(ui, padding, onOpenArc = { nav.navigate(ArcRoute(it)) }) }
            composable<StatsRoute> { StatsScreen(ui, padding) }
            composable<SettingsRoute> { SettingsScreen(ui, vm, padding, onBack = { nav.popBackStack() }) }
            composable<ArcRoute> { back ->
                val slug = back.toRoute<ArcRoute>().slug
                ArcDetailScreen(
                    ui, slug, padding,
                    onBack = { nav.popBackStack() },
                    onOpenEpisode = { sheetEpisode = it },
                    onToggleWatched = { vm.toggleWatched(it) },
                    onMarkUpTo = { vm.markUpTo(it) },
                    onMarkArc = { vm.markArc(slug, it) },
                )
            }
        }
    }

    sheetEpisode?.let { id ->
        EpisodeSheet(
            ui, id,
            onDismiss = { sheetEpisode = null },
            onPlay = play,
            onOpenWeb = { ep, lang, _ -> openWeb(ep, lang) },
            onToggleWatched = { vm.toggleWatched(it.id) },
        )
    }
}

private fun androidx.navigation.NavDestination.hierarchy() = generateSequence(this) { it.parent }
