package com.centralia.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role

/**
 * True when the user has turned animations off on the device. This is the
 * closest Android equivalent of SwiftUI's
 * `@Environment(\.accessibilityReduceMotion)`, which `CentraliaPressStyle`
 * consults before scaling.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * `CentraliaPressStyle` as a modifier: 0.78 opacity while pressed, a 0.985
 * scale unless motion is reduced, both eased out over 0.12s. Applying it also
 * makes the element clickable with no Material ripple, matching the SwiftUI
 * button which draws no platform highlight of its own.
 */
@Composable
fun Modifier.centraliaPressable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val pressedAndEnabled = isPressed && enabled

    val pressAlpha by animateFloatAsState(
        targetValue = if (pressedAndEnabled) 0.78f else 1f,
        animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
        label = "centraliaPressAlpha"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressedAndEnabled && !reduceMotion) 0.985f else 1f,
        animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
        label = "centraliaPressScale"
    )

    return this
        .graphicsLayer {
            alpha = pressAlpha
            scaleX = pressScale
            scaleY = pressScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * The `.opacity(isDisabled ? 0.48 : 1)` / `.opacity(canSave ? 1 : 0.45)`
 * treatment the SwiftUI buttons apply on top of the press style.
 */
fun Modifier.disabledAlpha(isDisabled: Boolean, disabledAlpha: Float = 0.48f): Modifier =
    if (isDisabled) this.alpha(disabledAlpha) else this
