package com.centralia.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.centralia.app.domain.greeting.TimeOfDayGreeting
import com.centralia.app.ui.theme.CentraliaColors
import com.centralia.app.ui.theme.CentraliaType
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * "Good evening, David" at the top of the Library — the first thing people see
 * after signing in. It uses the phone's clock and time zone (always in English), and
 * updates by itself on the minute (so it turns from afternoon to evening while
 * the app is open) and whenever the app comes back to the foreground (e.g.
 * after the user lands in another time zone).
 */
@Composable
fun LibraryGreeting(
    displayName: String?,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            // Wake up at the start of the next minute.
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            now = LocalTime.now()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) now = LocalTime.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val greeting = remember(now.hour, displayName) {
        TimeOfDayGreeting.greeting(now, displayName)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = greeting.title,
            style = CentraliaType.sectionTitle,
            color = CentraliaColors.Ink,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = greeting.subtitle,
            style = CentraliaType.subheadline,
            color = CentraliaColors.SecondaryText
        )
    }
}
