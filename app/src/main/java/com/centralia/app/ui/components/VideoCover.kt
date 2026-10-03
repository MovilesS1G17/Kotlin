package com.centralia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * A short's cover image, cropped to fill. While it loads, or if it can't be
 * loaded (an expired TikTok/Instagram link, no network), nothing is drawn, so
 * the platform colour underneath shows as before.
 */
@Composable
fun VideoCoverImage(
    url: String?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (url == null) return
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

/** Darkens the bottom of a cover so the white badges stay readable. */
@Composable
fun VideoCoverGradient(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.55f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.45f)
            )
        )
    )
}
