package com.centralia.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * `@ScaledMetric(relativeTo: .body) private var previewHeight: CGFloat = 150`
 *
 * SwiftUI grows such a value with the user's Dynamic Type setting so a card can
 * still hold its text at large sizes. Compose has no equivalent, so the base
 * value is multiplied by the current font scale, which is the same input.
 */
@Composable
fun scaledDp(base: Dp): Dp = base * LocalDensity.current.fontScale
