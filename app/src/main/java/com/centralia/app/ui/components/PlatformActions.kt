package com.centralia.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * The Android counterparts of the three UIKit hand-offs the SwiftUI code relies
 * on: `ShareLink`, `@Environment(\.openURL)`, and the `UIActivityViewController`
 * that shares the library export.
 */

/** `ShareLink(item:subject:message:)` — a plain-text share chooser. */
@Composable
fun rememberShareLink(): (subject: String, message: String, url: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { subject, message, url ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "$message\n$url")
            }
            context.startChooser(intent, title = "Share Link")
        }
    }
}

/**
 * `openURL(url) { accepted in … }` — returns false when nothing on the device can
 * handle the link, which is what drives the "Couldn't open this video" alert.
 */
@Composable
fun rememberOpenUrl(): (url: String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
        }
    }
}

/**
 * `ActivityShareSheet(items: [file.url])`. The export lives in the app's private
 * cache, so it is handed out as a `FileProvider` content URI with read
 * permission attached rather than a raw file path.
 */
@Composable
fun rememberShareFile(): (file: File) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { file ->
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startChooser(intent, title = "Export Library Data")
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}

private fun Context.startChooser(intent: Intent, title: String) {
    val chooser = Intent.createChooser(intent, title)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        // Nothing can receive the share; matching iOS, this fails quietly.
    }
}
