package com.centralia.app.ui.components

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Plays a short in-app through the platform's embeddable player
 * (YouTube embed, TikTok's official player, Instagram's embed page).
 *
 * The player is an `<iframe>` inside a tiny page loaded with the app id as its
 * base URL. YouTube refuses embeds without a Referer (errors 152/153) and asks
 * apps to identify themselves with their app id, which this does. The iOS
 * app's `ShortPlayerView` does the same with a `WKWebView`.
 */
@Composable
fun ShortPlayer(
    embedURL: String,
    modifier: Modifier = Modifier
) {
    val isLoading = remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Pause audio and video when the app goes to the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webView?.onPause()
                Lifecycle.Event.ON_RESUME -> webView?.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { context -> createPlayerWebView(context, isLoading).also { webView = it } },
            update = { view ->
                if (view.tag != embedURL) {
                    view.tag = embedURL
                    isLoading.value = true
                    view.loadDataWithBaseURL(
                        PLAYER_BASE_URL,
                        playerHtml(embedURL),
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            onRelease = { view ->
                view.stopLoading()
                view.loadUrl("about:blank")
                view.destroy()
                webView = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading.value) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private const val PLAYER_BASE_URL = "https://com.centralia.app/"
private const val PLAYER_HOST = "com.centralia.app"

@SuppressLint("SetJavaScriptEnabled")
private fun createPlayerWebView(context: Context, isLoading: MutableState<Boolean>): WebView =
    WebView(context).apply {
        setBackgroundColor(android.graphics.Color.BLACK)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        // target="_blank" links then arrive in shouldOverrideUrlLoading below.
        settings.setSupportMultipleWindows(false)

        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // The player's own frames load normally. Anything that tries to
                // replace the whole page ("Watch on YouTube", "View on Instagram",
                // the TikTok app link) opens in the platform's app or the browser.
                if (view == null || request == null || !request.isForMainFrame) return false
                val url = request.url ?: return false
                if (url.host == PLAYER_HOST || url.scheme == "about" || url.scheme == "data") return false
                openExternally(view.context, url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url?.startsWith(PLAYER_BASE_URL) == true) isLoading.value = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoading.value = false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) isLoading.value = false
            }
        }
    }

private fun openExternally(context: Context, url: Uri) {
    val intent = if (url.scheme == "intent") {
        // Web content must only reach other apps' public link handlers, never
        // a specific (possibly private) component of this app.
        runCatching { Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME) }.getOrNull()?.apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            component = null
            selector = null
            flags = flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                ).inv()
        }
    } else {
        Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE)
    } ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No app for that link (e.g. a deep link into an app that isn't installed).
    } catch (e: SecurityException) {
        // The target app doesn't accept outside links.
    }
}

private fun playerHtml(embedURL: String): String {
    val src = embedURL
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    return """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
        <style>html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden}
        iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style>
        </head><body>
        <iframe src="$src" allow="autoplay; encrypted-media; picture-in-picture; fullscreen; clipboard-write; web-share"
        allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe>
        </body></html>
    """.trimIndent()
}

/**
 * Paints the page colour over the four corners of a rectangle, so a view that
 * can't be clipped (a WebView) still looks like it has rounded corners.
 * It only draws; touches pass through to the player underneath.
 */
@Composable
fun RoundedCornerMask(
    radius: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = radius.toPx()
        val mask = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(r, r)))
        }
        drawPath(mask, color)
    }
}
