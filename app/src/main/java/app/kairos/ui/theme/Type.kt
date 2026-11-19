package app.kairos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.kairos.R

val Garamond = FontFamily(
    Font(R.font.eb_garamond_regular, FontWeight.Normal),
    Font(R.font.eb_garamond_medium, FontWeight.Medium),
    Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

val Inter = FontFamily(Font(R.font.inter, FontWeight.Normal), Font(R.font.inter, FontWeight.Medium))

private fun heading(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Garamond, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

val KairosTypography = Typography(
    displayLarge = heading(57, 64),
    displayMedium = heading(45, 52),
    displaySmall = heading(36, 44),
    headlineLarge = heading(32, 40),
    headlineMedium = heading(28, 36),
    headlineSmall = heading(24, 32),
    titleLarge = heading(22, 28, FontWeight.Medium),
    titleMedium = body(16, 24, FontWeight.Medium),
    titleSmall = body(14, 20, FontWeight.Medium),
    bodyLarge = body(17, 26),
    bodyMedium = body(15, 22),
    bodySmall = body(13, 18),
    labelLarge = body(15, 20, FontWeight.Medium),
    labelMedium = body(13, 16, FontWeight.Medium),
    labelSmall = body(11, 16, FontWeight.Medium),
)

/** Very large text for panic screens, where reading must take no effort. */
object PanicText {
    val Phase = TextStyle(fontFamily = Garamond, fontSize = 56.sp, lineHeight = 64.sp)
    val Prompt = TextStyle(fontFamily = Garamond, fontSize = 40.sp, lineHeight = 50.sp)
    val Card = TextStyle(fontFamily = Garamond, fontSize = 34.sp, lineHeight = 44.sp)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp)
}
