package com.myonepacelist

import android.app.Application
import com.myonepacelist.data.catalog.CatalogRepository
import com.myonepacelist.data.progress.StateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.myonepacelist.widget.NextEpisodeWidget
import com.myonepacelist.work.CatalogSyncWorker
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency container: the app is small enough not to need a DI framework. */
class AppContainer(app: Application) {
    /** Outlives screens: used for writes that must finish (e.g. saving the last playback position). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val catalog = CatalogRepository(app, http, BuildConfig.CATALOG_URL)
    val state = StateStore(app)
}

class MyOnePaceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CatalogSyncWorker.schedule(this)
        // Keep the home-screen widget in sync with progress and the preferred language.
        container.scope.launch {
            container.state.state
                .map { Triple(it.onboarded, it.progress, it.settings) }
                .distinctUntilChanged()
                .collect { NextEpisodeWidget.refresh(this@MyOnePaceApp) }
        }
    }
}

val android.content.Context.container: AppContainer
    get() = (applicationContext as MyOnePaceApp).container
