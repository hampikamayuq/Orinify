package com.zionhuang.music.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.utils.hasAuthenticatedSession
import com.zionhuang.music.LocalPlayerAwareWindowInsets
import com.zionhuang.music.R
import com.zionhuang.music.constants.*
import com.zionhuang.music.ui.component.IconButton
import com.zionhuang.music.ui.utils.backToMain
import com.zionhuang.music.utils.dataStore
import dev.diego.orinify.auth.LoginOrigin
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONTokener

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var disposed by remember { mutableStateOf(false) }
    val loginUrl = "https://accounts.google.com/AccountChooser?continue=https%3A%2F%2Fmusic.youtube.com%2F&service=youtube"

    DisposableEffect(Unit) {
        onDispose {
            disposed = true
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Column(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.login)) },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
            },
        )
        Text(stringResource(R.string.login_session_help), Modifier.padding(horizontal = 16.dp))
        message?.let { Text(it, Modifier.padding(16.dp)) }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = !busy, onClick = { message = null; webView?.loadUrl(loginUrl) }) {
                Text(stringResource(R.string.login_switch_account))
            }
            Button(enabled = ready && !busy, onClick = {
                val view = webView ?: return@Button
                if (!LoginOrigin.isMusic(view.url)) return@Button
                busy = true
                message = null
                // No JavaScript bridge is exposed to Google sign-in pages or third-party frames.
                view.evaluateJavascript(
                    """(function() { var c = window.ytcfg; return JSON.stringify({
                        loggedIn: !!(c && c.get('LOGGED_IN')),
                        visitorData: c && c.get('VISITOR_DATA')
                    }); })();"""
                ) { raw ->
                    if (disposed) return@evaluateJavascript
                    val snapshot = runCatching {
                        JSONObject(JSONTokener(raw).nextValue() as String)
                    }.getOrNull()
                    val visitor = snapshot?.optString("visitorData").orEmpty()
                    val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com/").orEmpty()
                    if (!LoginOrigin.isMusic(view.url) || snapshot?.optBoolean("loggedIn") != true ||
                        visitor.isBlank() || visitor == "null" || !hasAuthenticatedSession(cookie)
                    ) {
                        busy = false
                        message = context.getString(R.string.login_session_incomplete)
                        return@evaluateJavascript
                    }
                    scope.launch {
                        try {
                            YouTube.validateSession(cookie, visitor).fold(
                                onSuccess = { account ->
                                    context.dataStore.edit {
                                        it[InnerTubeCookieKey] = cookie
                                        it[VisitorDataKey] = visitor
                                        it[AccountNameKey] = account.name
                                        it[AccountEmailKey] = account.email.orEmpty()
                                        it[AccountChannelHandleKey] = account.channelHandle.orEmpty()
                                    }
                                    YouTube.cookie = cookie
                                    YouTube.visitorData = visitor
                                    navController.navigateUp()
                                },
                                onFailure = {
                                    // Network exceptions can contain credentials: show only a fixed message.
                                    message = context.getString(R.string.login_session_failed)
                                },
                            )
                        } finally {
                            busy = false
                        }
                    }
                }
            }) {
                Text(stringResource(if (busy) R.string.login_session_checking else R.string.login_connect_account))
            }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    CookieManager.getInstance().setAcceptCookie(true)
                    webViewClient = object : WebViewClient() {
                        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                            canGoBack = view.canGoBack()
                            ready = LoginOrigin.isMusic(url)
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            canGoBack = view.canGoBack()
                            ready = LoginOrigin.isMusic(url) && LoginOrigin.isMusic(view.url)
                        }
                    }
                    webView = this
                    loadUrl(loginUrl)
                }
            },
        )
    }
    BackHandler(enabled = canGoBack && !busy) { webView?.goBack() }
}
