package com.centralia.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.centralia.app.R

/**
 * The two bundled faces declared in `Info.plist` under `UIAppFonts`. The
 * SwiftUI theme falls back to a system serif when `UIFont(name:)` returns nil;
 * on Android a missing font resource is a compile error instead, so the files
 * themselves are the guarantee and [FontFamily.Serif] is only used for the
 * generic serif fallback below.
 */
object CentraliaFonts {
    val Joan = FontFamily(Font(R.font.joan_regular, FontWeight.Normal))
    val InstrumentSerif = FontFamily(Font(R.font.instrument_serif_regular, FontWeight.Normal))
    val Serif = FontFamily.Serif
}

/**
 * iOS text styles have no Compose equivalent, so each one the app uses is
 * reproduced here at its default (Large) Dynamic Type size. Sizes are declared
 * in `sp`, which means Android's font-size accessibility setting scales them
 * just as Dynamic Type scales the originals.
 *
 * | SwiftUI          | size / weight |
 * |------------------|---------------|
 * | .largeTitle      | 34 regular    |
 * | .title           | 28 regular    |
 * | .title2          | 22 regular    |
 * | .title3          | 20 regular    |
 * | .headline        | 17 semibold   |
 * | .body            | 17 regular    |
 * | .subheadline     | 15 regular    |
 * | .footnote        | 13 regular    |
 * | .caption         | 12 regular    |
 * | .caption2        | 11 regular    |
 */
object CentraliaType {
    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Normal)
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
    val title2 = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal)
    val headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val caption2 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Normal)

    /** `CentraliaTheme.Typography.brand` — Joan-Regular at 30. */
    val brand = TextStyle(
        fontFamily = CentraliaFonts.Joan,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Normal
    )

    /** `CentraliaTheme.Typography.display` — InstrumentSerif-Regular at 35. */
    val display = TextStyle(
        fontFamily = CentraliaFonts.InstrumentSerif,
        fontSize = 35.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Normal
    )

    /** `CentraliaTheme.Typography.sectionTitle` — InstrumentSerif-Regular at 24. */
    val sectionTitle = TextStyle(
        fontFamily = CentraliaFonts.InstrumentSerif,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal
    )
}

/** `.font(.subheadline.weight(.semibold))` and friends. */
fun TextStyle.semibold(): TextStyle = copy(fontWeight = FontWeight.SemiBold)

fun TextStyle.bold(): TextStyle = copy(fontWeight = FontWeight.Bold)

fun TextStyle.medium(): TextStyle = copy(fontWeight = FontWeight.Medium)
