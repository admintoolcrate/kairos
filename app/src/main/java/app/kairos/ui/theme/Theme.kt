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
    primary = Mono.Ink, onPrimary = Mono.Page,
    primaryContainer = Mono.Raised, onPrimaryContainer = Mono.Ink,
    secondary = Mono.Ink, onSecondary = Mono.Page,
    secondaryContainer = Mono.Tint, onSecondaryContainer = Mono.Ink,
    tertiary = Mono.Gold, onTertiary = Mono.Card,
    tertiaryContainer = Mono.Tint, onTertiaryContainer = Mono.Ink,
    background = Mono.Page, onBackground = Mono.Ink,
    surface = Mono.Page, onSurface = Mono.Ink,
    surfaceVariant = Mono.Raised, onSurfaceVariant = Mono.Muted,
    surfaceContainerLowest = Mono.Page, surfaceContainerLow = Mono.Card,
    surfaceContainer = Mono.Page, surfaceContainerHigh = Mono.High, surfaceContainerHighest = Mono.Raised,
    outline = Mono.Line, outlineVariant = Mono.Hairline,
    // No red: an error is said in words, in ink.
    error = Mono.Ink, onError = Mono.Page, errorContainer = Mono.Tint, onErrorContainer = Mono.Ink,
    inverseSurface = Mono.Ink, inverseOnSurface = Mono.Page, inversePrimary = Mono.NightInk,
    surfaceTint = Mono.Page, scrim = Mono.Ink,
)

private val DarkColors = darkColorScheme(
    primary = Mono.NightInk, onPrimary = Mono.NightPage,
    primaryContainer = Mono.NightRaised, onPrimaryContainer = Mono.NightInk,
    secondary = Mono.NightInk, onSecondary = Mono.NightPage,
    secondaryContainer = Mono.NightTint, onSecondaryContainer = Mono.NightInk,
    tertiary = Mono.NightGold, onTertiary = Mono.NightPage,
    tertiaryContainer = Mono.NightTint, onTertiaryContainer = Mono.NightInk,
    background = Mono.NightPage, onBackground = Mono.NightInk,
    surface = Mono.NightPage, onSurface = Mono.NightInk,
    surfaceVariant = Mono.NightRaised, onSurfaceVariant = Mono.NightMuted,
    surfaceContainerLowest = Mono.NightPage, surfaceContainerLow = Mono.NightCard,
    surfaceContainer = Mono.NightPage, surfaceContainerHigh = Mono.NightHigh, surfaceContainerHighest = Mono.NightRaised,
    outline = Mono.NightLine, outlineVariant = Mono.NightHairline,
    error = Mono.NightInk, onError = Mono.NightPage, errorContainer = Mono.NightTint, onErrorContainer = Mono.NightInk,
    inverseSurface = Mono.NightInk, inverseOnSurface = Mono.NightPage, inversePrimary = Mono.Ink,
    surfaceTint = Mono.NightPage, scrim = Mono.NightPage,
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
