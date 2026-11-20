package app.kairos.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrgeTimerTest {
    private val min = 60_000L

    @Test fun `counts down from the start`() {
        val t = UrgeTimer.start(10, now = 1_000)
        assertEquals(10 * min, t.remaining(1_000))
        assertEquals(9 * min, t.remaining(1_000 + min))
        assertEquals(0.1f, t.progress(1_000 + min), 0.0001f)
    }

    @Test fun `arrives at zero and stays there - no overtime`() {
        val t = UrgeTimer.start(10, now = 0)
        assertTrue(t.isFinished(10 * min))
        assertEquals(0L, t.remaining(30 * min))
        assertEquals(1f, t.progress(30 * min), 0f)
    }

    @Test fun `pause freezes the clock and resume continues`() {
        var t = UrgeTimer.start(20, now = 0)
        t = t.pause(5 * min)
        assertTrue(t.isPaused)
        assertEquals(15 * min, t.remaining(12 * min))
        t = t.resume(12 * min)
        assertFalse(t.isPaused)
        assertEquals(15 * min, t.remaining(12 * min))
        assertEquals(10 * min, t.remaining(17 * min))
    }

    @Test fun `double pause and resume without pause are harmless`() {
        val t = UrgeTimer.start(10, 0).pause(min)
        assertEquals(t, t.pause(2 * min))
        val r = UrgeTimer.start(10, 0)
        assertEquals(r, r.resume(min))
    }

    @Test fun `extend adds five minutes, even after finishing`() {
        val t = UrgeTimer.start(10, 0).extend(UrgeTimer.EXTEND_MINUTES * min)
        assertEquals(15 * min, t.remaining(0))
        val finished = UrgeTimer.start(10, 0)
        assertEquals(5 * min, finished.extend(5 * min).remaining(10 * min))
    }

    @Test fun `format rounds up and pads seconds`() {
        assertEquals("10:00", UrgeTimer.format(10 * min))
        assertEquals("9:59", UrgeTimer.format(10 * min - 1_000))
        assertEquals("0:01", UrgeTimer.format(1))
        assertEquals("0:00", UrgeTimer.format(0))
    }

    @Test fun `choices are ten and twenty minutes`() {
        assertEquals(listOf(10, 20), UrgeTimer.CHOICES_MINUTES)
    }
}
