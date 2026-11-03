package com.myonepacelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.myonepacelist.ui.AppViewModel
import com.myonepacelist.ui.MainScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val openEpisode = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !vm.ui.value.ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent { MainScreen(vm, openEpisode.receiveAsFlow()) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(EXTRA_EPISODE)?.let { openEpisode.trySend(it) }
    }

    companion object {
        const val EXTRA_EPISODE = "episode"

        /** Opens the app on an episode's detail sheet. */
        fun episodeIntent(context: Context, episodeId: String) =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_EPISODE, episodeId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
