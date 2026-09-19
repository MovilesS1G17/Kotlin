package com.centralia.app.ui.theme

import androidx.compose.ui.unit.dp

/** `CentraliaTheme.Spacing` from the SwiftUI design system, 1 pt -> 1 dp. */
object Spacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
    val xxLarge = 48.dp
}

/** `CentraliaTheme.Radius`. */
object Radius {
    val control = 14.dp
    val sheet = 24.dp
}

/**
 * The SwiftUI layouts cap their content with `.frame(maxWidth: 700)` (660 on the
 * save confirmation, 440 on the auth scaffold) and centre it. These reproduce
 * that on large-screen Android devices and foldables.
 */
object ContentWidth {
    val standard = 700.dp
    val confirmation = 660.dp
    val auth = 440.dp
}
