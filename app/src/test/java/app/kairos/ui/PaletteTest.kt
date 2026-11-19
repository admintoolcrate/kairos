package app.kairos.ui

import androidx.compose.ui.graphics.Color
import app.kairos.ui.theme.Paper
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** "No red alerts": nothing in the Paper palette is a saturated red. */
class PaletteTest {
    private fun hueAndSaturation(c: Color): Pair<Float, Float> {
        val r = c.red; val g = c.green; val b = c.blue
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn
        if (d == 0f) return 0f to 0f
        val h = when (mx) {
            r -> 60 * (((g - b) / d) % 6)
            g -> 60 * (((b - r) / d) + 2)
            else -> 60 * (((r - g) / d) + 4)
        }.let { if (it < 0) it + 360 else it }
        return h to d / mx
    }

    @Test fun `no colour in the palette is a saturated red`() {
        val all = Paper::class.java.declaredFields.filter { it.type == Long::class.javaPrimitiveType }.map {
            it.isAccessible = true
            it.name to Color(it.getLong(Paper).toULong())
        }
        check(all.size >= 20) { "expected the Paper colours, found ${all.size}" }
        all.forEach { (name, c) ->
            val (h, s) = hueAndSaturation(c)
            val red = (h < 15f || h > 345f) && s > 0.45f
            assertFalse("$name is too close to an alarm red (hue $h, saturation $s)", red)
        }
    }
}
