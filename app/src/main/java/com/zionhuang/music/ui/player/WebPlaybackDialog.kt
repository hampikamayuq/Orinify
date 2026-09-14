package com.zionhuang.music.ui.player

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import com.zionhuang.music.R
import dev.diego.orinify.auth.WebPlaybackUrl

/** Visible first-party player. Uses WebView's existing login without extracting media URLs. */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPlaybackDialog(videoId: String, nativePlayer: Player, onDismiss: () -> Unit) {
    val url = remember(videoId) { WebPlaybackUrl.forVideo(videoId) } ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dismiss by rememberUpdatedState(onDismiss)
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(nativePlayer, lifecycle) {
        nativePlayer.pause()
        val playerListener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) nativePlayer.pause()
            }
        }
        nativePlayer.addListener(playerListener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) dismiss()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            nativePlayer.removeListener(playerListener)
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.onPause()
            webView?.destroy()
            webView = null
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.web_playback_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(painterResource(R.drawable.close), stringResource(R.string.web_playback_close))
                        }
                    },
                )
                Text(stringResource(R.string.web_playback_help), Modifier.padding(16.dp))
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mediaPlaybackRequiresUserGesture = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            webView = this
                            loadUrl(url)
                        }
                    },
                )
            }
        }
    }
}
