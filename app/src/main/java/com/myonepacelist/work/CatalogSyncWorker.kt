package com.myonepacelist.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.myonepacelist.MainActivity
import com.myonepacelist.R
import com.myonepacelist.container
import com.myonepacelist.data.catalog.Catalog
import com.myonepacelist.domain.CatalogDiff
import com.myonepacelist.domain.catalogDiff
import com.myonepacelist.widget.NextEpisodeWidget
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Once a day: downloads the catalog the GitHub Action keeps fresh and notifies about new episodes or dubs. */
class CatalogSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val change = try {
            c.catalog.refresh()
        } catch (_: IOException) {
            return if (runAttemptCount < 3) Result.retry() else Result.success()
        } ?: return Result.success()

        val (old, new) = change
        val state = c.state.current()
        if (state.knownCatalogVersion != new.version) {
            val diff = catalogDiff(old, new)
            if (state.settings.notifications && !diff.isEmpty) notify(applicationContext, new, diff)
            c.state.update { it.copy(knownCatalogVersion = new.version) }
        }
        NextEpisodeWidget.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK = "catalog-sync"
        private const val CHANNEL = "new-episodes"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun notify(context: Context, catalog: Catalog, diff: CatalogDiff) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Episodios nuevos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Avisos cuando One Pace publica episodios o doblaje nuevo"
                },
            )
            val first = diff.newEpisodes.firstOrNull() ?: diff.newDubs.first()
            val arc = catalog.arc(first.arc)?.title.orEmpty()
            val (title, text) = when {
                diff.newEpisodes.size == 1 && diff.newDubs.isEmpty() ->
                    "Nuevo episodio: $arc ${first.number}" to first.title
                diff.newEpisodes.isNotEmpty() ->
                    "${diff.newEpisodes.size} episodios nuevos de One Pace" to
                        diff.newEpisodes.take(3).joinToString(" · ") { "${catalog.arc(it.arc)?.title} ${it.number}" }
                diff.newDubs.size == 1 -> "Ya hay doblaje latino: $arc ${first.number}" to first.title
                else -> "Doblaje latino nuevo en ${diff.newDubs.size} episodios" to
                    diff.newDubs.take(3).joinToString(" · ") { "${catalog.arc(it.arc)?.title} ${it.number}" }
            }
            val tap = PendingIntent.getActivity(
                context, 0, MainActivity.episodeIntent(context, first.id),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFF4B740.toInt())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, n)
        }
    }
}
