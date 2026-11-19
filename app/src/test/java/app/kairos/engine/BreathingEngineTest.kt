package app.kairos.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BreathingEngineTest {
    private val engine = BreathingEngine()

    @Test fun `default is four in and six out`() {
        assertEquals(4, BreathPattern.Default.inhaleSeconds)
        assertEquals(6, BreathPattern.Default.exhaleSeconds)
        assertEquals(10_000L, BreathPattern.Default.cycleMillis)
    }

    @Test fun `there are only inhale and exhale phases - never a hold`() {
        assertEquals(listOf(BreathingPhase.Inhale, BreathingPhase.Exhale), BreathingPhase.entries.toList())
        val seen = (0L until 60_000L step 50).map { engine.frameAt(it).phase }.toSet()
        assertEquals(setOf(BreathingPhase.Inhale, BreathingPhase.Exhale), seen)
    }

    @Test fun `expansion never plateaus at full - no hidden hold at the top`() {
        // A hold would show as the circle staying full; exhale must start shrinking at once.
        val top = engine.frameAt(4_000).expansion
        val justAfter = engine.frameAt(4_300).expansion
        assertTrue(justAfter < top)
    }

    @Test fun `phase boundaries at 4s and 10s`() {
        assertEquals(BreathingPhase.Inhale, engine.frameAt(0).phase)
        assertEquals(BreathingPhase.Inhale, engine.frameAt(3_999).phase)
        assertEquals(BreathingPhase.Exhale, engine.frameAt(4_000).phase)
        assertEquals(BreathingPhase.Exhale, engine.frameAt(9_999).phase)
        assertEquals(BreathingPhase.Inhale, engine.frameAt(10_000).phase)
    }

    @Test fun `cycles count completed breaths`() {
        assertEquals(0, engine.frameAt(9_999).cycle)
        assertEquals(1, engine.frameAt(10_000).cycle)
        assertEquals(6, engine.frameAt(60_000).cycle)
    }

    @Test fun `expansion rises on inhale and falls on exhale`() {
        assertEquals(0f, engine.frameAt(0).expansion, 0.001f)
        assertEquals(0.5f, engine.frameAt(2_000).expansion, 0.001f)
        assertEquals(1f, engine.frameAt(4_000).expansion, 0.001f)
        assertEquals(0.5f, engine.frameAt(7_000).expansion, 0.001f)
        assertTrue(engine.frameAt(9_999).expansion < 0.01f)
    }

    @Test fun `seconds left counts down within a phase`() {
        assertEquals(4, engine.frameAt(0).secondsLeftInPhase)
        assertEquals(1, engine.frameAt(3_500).secondsLeftInPhase)
        assertEquals(6, engine.frameAt(4_000).secondsLeftInPhase)
        assertEquals(1, engine.frameAt(9_500).secondsLeftInPhase)
    }

    @Test fun `negative time is treated as the start`() {
        assertEquals(engine.frameAt(0), engine.frameAt(-500))
    }

    @Test fun `exhale must be longer than inhale`() {
        listOf(4 to 4, 5 to 4, 6 to 3).forEach { (i, o) ->
            try {
                BreathPattern(i, o)
                fail("$i/$o should be rejected")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test fun `of clamps input and always lengthens the exhale`() {
        assertEquals(BreathPattern(4, 5), BreathPattern.of(4, 4))
        assertEquals(BreathPattern(6, 7), BreathPattern.of(6, 2))
        assertEquals(BreathPattern(2, 3), BreathPattern.of(0, 0))
        assertEquals(BreathPattern(8, 12), BreathPattern.of(30, 30))
        // Every possible clamped pattern keeps exhale > inhale.
        for (i in -2..20) for (o in -2..20) {
            val p = BreathPattern.of(i, o)
            assertTrue("$i/$o -> $p", p.exhaleSeconds > p.inhaleSeconds)
        }
    }

    @Test fun `custom pattern changes timing`() {
        val slow = BreathingEngine(BreathPattern(5, 8))
        assertEquals(BreathingPhase.Inhale, slow.frameAt(4_999).phase)
        assertEquals(BreathingPhase.Exhale, slow.frameAt(5_000).phase)
        assertEquals(1, slow.frameAt(13_000).cycle)
    }

    @Test fun `phase change detection for haptics`() {
        assertTrue(engine.phaseChangedBetween(3_900, 4_100))
        assertTrue(engine.phaseChangedBetween(9_900, 10_100))
        assertFalse(engine.phaseChangedBetween(1_000, 2_000))
        assertFalse(engine.phaseChangedBetween(5_000, 5_000))
    }

    @Test fun `a minute is six breaths at the default`() {
        assertEquals(60_000L, engine.durationOf(6))
    }
}
