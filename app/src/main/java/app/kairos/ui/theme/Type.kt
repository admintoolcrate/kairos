package app.kairos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.kairos.R

/**
 * Type follows the ToolCrate family (docs/family-look.md), as Stagira sets it:
 * EB Garamond for display moments only (the name, big figures, passages, screen titles),
 * Inter for everything else, its weights taken from the variable axis.
 */
val Garamond = FontFamily(
    Font(R.font.eb_garamond_regular, FontWeight.Normal),
    Font(R.font.eb_garamond_medium, FontWeight.Medium),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    inter(FontWeight.Light),
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
)

val KairosTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Garamond, fontWeight = FontWeight.Normal, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
        displayMedium = displayMedium.copy(fontFamily = Garamond, fontWeight = FontWeight.Normal, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"),
        displaySmall = displaySmall.copy(fontFamily = Garamond, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, fontFeatureSettings = "tnum"),
        headlineLarge = headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Light, letterSpacing = (-0.6).sp),
        headlineMedium = headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Light, letterSpacing = (-0.4).sp),
        headlineSmall = headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp),
        titleLarge = titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        titleMedium = titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
        titleSmall = titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Inter, fontSize = 17.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(fontFamily = Inter, letterSpacing = 0.sp),
        bodySmall = bodySmall.copy(fontFamily = Inter, letterSpacing = 0.1.sp),
        labelLarge = labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
        labelSmall = labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    )
}

/** A screen's title, in Garamond, as Stagira sets its top bars (docs/family-look.md). */
val ScreenTitle = TextStyle(fontFamily = Garamond, fontSize = 30.sp, lineHeight = 34.sp)

/** Garamond sizes for Kairos's display moments. */
object KairosType {
    /** A calm line to read: an urge message, a reassurance card. */
    val passage = TextStyle(fontFamily = Garamond, fontStyle = FontStyle.Italic, fontSize = 24.sp, lineHeight = 32.sp)
}

/** Very large text for panic screens, where reading must take no effort. */
object PanicText {
    val Phase = TextStyle(fontFamily = Garamond, fontSize = 56.sp, lineHeight = 64.sp)
    val Prompt = TextStyle(fontFamily = Garamond, fontSize = 40.sp, lineHeight = 50.sp)
    val Card = TextStyle(fontFamily = Garamond, fontSize = 34.sp, lineHeight = 44.sp)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp)
}

/**
 * Big display type grows with the system font size only up to [max]x, so it never swamps
 * the screen; body text scales fully. (As in Stagira.)
 */
@Composable
fun TextStyle.capped(max: Float = 1.25f): TextStyle {
    val scale = LocalDensity.current.fontScale
    if (scale <= max) return this
    val k = max / scale
    return copy(fontSize = fontSize * k, lineHeight = if (lineHeight.isSp) lineHeight * k else lineHeight)
}
