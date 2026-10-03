package com.centralia.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

/**
 * The SwiftUI app never opts into a dark appearance and sets `.tint(Ink)` plus
 * `.foregroundStyle(Ink)` at the root of every screen. The Compose equivalent is
 * a single light scheme wired to the same palette, so Material components
 * (dialogs, switches, bottom sheets, text selection handles) pick up Centralia
 * colours instead of Material defaults, and dynamic colour stays off.
 */
private val CentraliaColorScheme = lightColorScheme(
    primary = CentraliaColors.Ink,
    onPrimary = CentraliaColors.Surface,
    primaryContainer = CentraliaColors.Ink,
    onPrimaryContainer = CentraliaColors.Surface,
    secondary = CentraliaColors.SecondaryText,
    onSecondary = CentraliaColors.Surface,
    secondaryContainer = CentraliaColors.SoftSurface,
    onSecondaryContainer = CentraliaColors.Ink,
    tertiary = CentraliaColors.VideoLavender,
    onTertiary = CentraliaColors.Ink,
    background = CentraliaColors.Canvas,
    onBackground = CentraliaColors.Ink,
    surface = CentraliaColors.Surface,
    onSurface = CentraliaColors.Ink,
    surfaceVariant = CentraliaColors.SoftSurface,
    onSurfaceVariant = CentraliaColors.SecondaryText,
    surfaceContainerLow = CentraliaColors.Surface,
    surfaceContainer = CentraliaColors.Surface,
    surfaceContainerHigh = CentraliaColors.Surface,
    outline = CentraliaColors.Divider,
    outlineVariant = CentraliaColors.Divider,
    error = CentraliaColors.Error,
    onError = CentraliaColors.Surface,
    scrim = CentraliaColors.Ink
)

/**
 * Material's type scale is mapped onto the iOS styles so any Material component
 * the app uses (dialog titles, sheet labels) reads in Centralia's voice.
 */
private val CentraliaMaterialTypography = Typography(
    displayLarge = CentraliaType.display,
    displayMedium = CentraliaType.display,
    displaySmall = CentraliaType.sectionTitle,
    headlineLarge = CentraliaType.largeTitle,
    headlineMedium = CentraliaType.title,
    headlineSmall = CentraliaType.title2,
    titleLarge = CentraliaType.title3,
    titleMedium = CentraliaType.headline,
    titleSmall = CentraliaType.subheadline.semibold(),
    bodyLarge = CentraliaType.body,
    bodyMedium = CentraliaType.subheadline,
    bodySmall = CentraliaType.footnote,
    labelLarge = CentraliaType.headline,
    labelMedium = CentraliaType.subheadline.semibold(),
    labelSmall = CentraliaType.caption.semibold()
)

@Composable
fun CentraliaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CentraliaColorScheme,
        typography = CentraliaMaterialTypography,
        content = content
    )
}

/** Shorthand used across the feature packages. */
val bodyStyle: TextStyle get() = CentraliaType.body
