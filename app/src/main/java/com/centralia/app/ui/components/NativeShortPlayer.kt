package com.centralia.app.ui.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.centralia.app.R

/**
 * Plays a short's MP4 from the Centralia backend with ExoPlayer — the
 * device's own video pipeline, so the picture shows on every phone and
 * emulator (unlike web embeds, which can give sound with a black picture).
 *
 * Loops like the platforms do, pauses when the app goes to the background,
 * and calls [onError] if the stream can't be played so the screen can fall
 * back to the platform's web player. The iOS app does the same with AVPlayer.
 */
@OptIn(UnstableApi::class)
@Composable
fun NativeShortPlayer(
    streamURL: String,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnError by rememberUpdatedState(onError)

    val player = remember(streamURL) {
        // The first request after a save can wait while the backend fetches the
        // video, so reads are allowed to take longer than ExoPlayer's 8 s default.
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(90_000)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http)))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(streamURL)
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                )
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                currentOnError()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pause in the background, carry on when the user comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        var resumeOnReturn = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumeOnReturn = player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> if (resumeOnReturn) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { viewContext ->
            (LayoutInflater.from(viewContext).inflate(R.layout.short_player_view, null, false) as PlayerView)
                .also { it.player = player }
        },
        update = { view -> if (view.player !== player) view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier
    )
}
