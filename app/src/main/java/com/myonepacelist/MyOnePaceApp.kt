package com.myonepacelist

import android.app.Application
import com.myonepacelist.data.catalog.CatalogRepository
import com.myonepacelist.data.progress.StateStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency container: the app is small enough not to need a DI framework. */
class AppContainer(app: Application) {
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
    }
}

val android.content.Context.container: AppContainer
    get() = (applicationContext as MyOnePaceApp).container
