package app.kairos.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mono: the ToolCrate family palette (docs/family-look.md), shared with Stagira, Lyceum and
 * Telos. Black on white, true black at night, one sparing gold accent.
 * There is no red anywhere: errors use ink, because nothing in Kairos should look like an alarm.
 */
object Mono {
    // Light
    val Page = Color(0xFFFFFFFF)
    val Card = Color(0xFFF7F7F6)
    val Raised = Color(0xFFF0F0EE)
    val High = Color(0xFFF3F3F2)
    val Tint = Color(0xFFECECEA)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF6E6E6E)
    val Line = Color(0xFFCFCFCC)
    val Hairline = Color(0xFFEAEAE8)
    val Gold = Color(0xFFA8834E)

    // Dark (true black)
    val NightPage = Color(0xFF000000)
    val NightCard = Color(0xFF0E0E0E)
    val NightRaised = Color(0xFF171717)
    val NightHigh = Color(0xFF121212)
    val NightTint = Color(0xFF262626)
    val NightInk = Color(0xFFFFFFFF)
    val NightMuted = Color(0xFF9A9A9A)
    val NightLine = Color(0xFF3A3A3A)
    val NightHairline = Color(0xFF1C1C1C)
    val NightGold = Color(0xFFD6B47A)
}
