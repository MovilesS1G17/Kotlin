package com.centralia.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType

/**
 * Eventual connectivity (Sprint 3): tells the user they are offline and what
 * still works, and how many changes are waiting to sync. Mirrors
 * `OfflineBanner` in the iOS app.
 */
@Composable
fun OfflineBanner(isOnline: Boolean, pendingChanges: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = !isOnline || pendingChanges > 0) {
        Text(
            text = offlineBannerText(isOnline, pendingChanges),
            style = CentraliaType.caption,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .background(CentraliaColors.Ink)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/** Pure text rule, shared with the tests (and with iOS). */
fun offlineBannerText(isOnline: Boolean, pendingChanges: Int): String {
    val changes = if (pendingChanges == 1) "1 change" else "$pendingChanges changes"
    return when {
        !isOnline && pendingChanges > 0 ->
            "You're offline. Showing your saved library · $changes will sync when you're back online."
        !isOnline -> "You're offline. Showing your saved library."
        else -> "Back online · syncing $changes…"
    }
}
