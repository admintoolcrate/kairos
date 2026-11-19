package app.kairos.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { System, Light, Dark }
enum class MotionPref { System, Reduced, Full }

/** True when animations should be replaced by still images and text. */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Paper.Ink, onPrimary = Paper.Page,
    primaryContainer = Paper.Raised, onPrimaryContainer = Paper.Ink,
    secondary = Paper.Accent, onSecondary = Paper.Card,
    secondaryContainer = Paper.Tint, onSecondaryContainer = Paper.Ink,
    tertiary = Paper.Accent, onTertiary = Paper.Card,
    tertiaryContainer = Paper.Tint, onTertiaryContainer = Paper.Ink,
    background = Paper.Page, onBackground = Paper.Ink,
    surface = Paper.Page, onSurface = Paper.Ink,
    surfaceVariant = Paper.Raised, onSurfaceVariant = Paper.Muted,
    surfaceContainerLowest = Paper.Card, surfaceContainerLow = Paper.Card,
    surfaceContainer = Paper.Card, surfaceContainerHigh = Paper.Raised, surfaceContainerHighest = Paper.Raised,
    outline = Paper.Line, outlineVariant = Paper.Hairline,
    error = Paper.Umber, onError = Paper.Card, errorContainer = Paper.Tint, onErrorContainer = Paper.Ink,
    inverseSurface = Paper.Ink, inverseOnSurface = Paper.Page, inversePrimary = Paper.NightAccent,
    surfaceTint = Paper.Page, scrim = Paper.Ink,
)

private val DarkColors = darkColorScheme(
    primary = Paper.NightInk, onPrimary = Paper.NightPage,
    primaryContainer = Paper.NightRaised, onPrimaryContainer = Paper.NightInk,
    secondary = Paper.NightAccent, onSecondary = Paper.NightPage,
    secondaryContainer = Paper.NightTint, onSecondaryContainer = Paper.NightInk,
    tertiary = Paper.NightAccent, onTertiary = Paper.NightPage,
    tertiaryContainer = Paper.NightTint, onTertiaryContainer = Paper.NightInk,
    background = Paper.NightPage, onBackground = Paper.NightInk,
    surface = Paper.NightPage, onSurface = Paper.NightInk,
    surfaceVariant = Paper.NightRaised, onSurfaceVariant = Paper.NightMuted,
    surfaceContainerLowest = Paper.NightCard, surfaceContainerLow = Paper.NightCard,
    surfaceContainer = Paper.NightCard, surfaceContainerHigh = Paper.NightRaised, surfaceContainerHighest = Paper.NightRaised,
    outline = Paper.NightLine, outlineVariant = Paper.NightHairline,
    error = Paper.NightUmber, onError = Paper.NightPage, errorContainer = Paper.NightTint, onErrorContainer = Paper.NightInk,
    inverseSurface = Paper.NightInk, inverseOnSurface = Paper.NightPage, inversePrimary = Paper.Accent,
    surfaceTint = Paper.NightPage, scrim = Paper.NightPage,
)

@Composable
fun KairosTheme(
    mode: ThemeMode = ThemeMode.System,
    motion: MotionPref = MotionPref.System,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val systemReduced = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val reduced = when (motion) {
        MotionPref.System -> systemReduced
        MotionPref.Reduced -> true
        MotionPref.Full -> false
    }
    CompositionLocalProvider(LocalReducedMotion provides reduced) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = KairosTypography,
            content = content,
        )
    }
}
