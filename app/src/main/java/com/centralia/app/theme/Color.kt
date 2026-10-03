package com.centralia.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Direct translation of `Assets.xcassets` colour sets. Each sRGB component from
 * the iOS asset catalog is converted to 8-bit (component * 255, rounded):
 *
 * WarmCanvas    0.988 / 0.984 / 0.957 -> #FCFBF4
 * Ink           0.067 / 0.067 / 0.067 -> #111111
 * Surface       1.000 / 1.000 / 1.000 -> #FFFFFF
 * SoftSurface   0.945 / 0.937 / 0.906 -> #F1EFE7
 * Divider       0.847 / 0.831 / 0.784 -> #D8D4C8
 * SecondaryText 0.435 / 0.420 / 0.388 -> #6F6B63
 * VideoSand     0.839 / 0.796 / 0.745 -> #D6CBBE
 * VideoMint     0.784 / 0.843 / 0.816 -> #C8D7D0
 * VideoClay     0.886 / 0.824 / 0.741 -> #E2D2BD
 * VideoLavender 0.808 / 0.788 / 0.851 -> #CEC9D9
 *
 * The iOS catalog defines a single universal appearance for every colour (no
 * dark variant), so the Compose palette is likewise appearance-independent and
 * the app renders identically regardless of the system dark-mode setting.
 */
object CentraliaColors {
    val Canvas = Color(0xFFFCFBF4)
    val Ink = Color(0xFF111111)
    val Surface = Color(0xFFFFFFFF)
    val SoftSurface = Color(0xFFF1EFE7)
    val Divider = Color(0xFFD8D4C8)
    val SecondaryText = Color(0xFF6F6B63)
    val VideoSand = Color(0xFFD6CBBE)
    val VideoMint = Color(0xFFC8D7D0)
    val VideoClay = Color(0xFFE2D2BD)
    val VideoLavender = Color(0xFFCEC9D9)

    /** SwiftUI `.red` for inline validation errors. */
    val Error = Color(0xFFFF3B30)

    /** Ordered exactly as `LibraryVideoCard.cardColor` indexes them. */
    val videoCardPalette = listOf(VideoSand, VideoMint, VideoClay, VideoLavender)

    /** Ordered as `FolderDetailVideoCard.cardColor` indexes them. */
    val folderCardPalette = listOf(VideoClay, VideoMint, VideoSand, VideoLavender)
}
