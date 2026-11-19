package app.kairos.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundingEngineTest {

    @Test fun `senses go see touch hear smell taste with 5 4 3 2 1`() {
        assertEquals(listOf(Sense.See, Sense.Touch, Sense.Hear, Sense.Smell, Sense.Taste), Sense.entries.toList())
        assertEquals(listOf(5, 4, 3, 2, 1), Sense.entries.map { it.count })
    }

    @Test fun `fifteen prompts in total, one per screen`() {
        assertEquals(15, GroundingWalkthrough.TOTAL)
        assertEquals((0 until 15).toList(), GroundingWalkthrough.PROMPTS.map { it.index })
    }

    @Test fun `walking forward visits every prompt in order`() {
        var w = GroundingWalkthrough()
        val visited = mutableListOf<Pair<Sense, Int>>()
        while (!w.isComplete) {
            val p = w.current!!
            visited += p.sense to p.item
            w = w.next()
        }
        assertEquals(
            listOf(
                Sense.See to 1, Sense.See to 2, Sense.See to 3, Sense.See to 4, Sense.See to 5,
                Sense.Touch to 1, Sense.Touch to 2, Sense.Touch to 3, Sense.Touch to 4,
                Sense.Hear to 1, Sense.Hear to 2, Sense.Hear to 3,
                Sense.Smell to 1, Sense.Smell to 2,
                Sense.Taste to 1,
            ),
            visited,
        )
        assertNull(w.current)
        assertEquals(1f, w.progress, 0f)
    }

    @Test fun `first prompt of each sense is flagged`() {
        val firsts = GroundingWalkthrough.PROMPTS.filter { it.isFirstOfSense }.map { it.sense }
        assertEquals(Sense.entries.toList(), firsts)
    }

    @Test fun `back at the start stays put`() {
        val w = GroundingWalkthrough()
        assertTrue(w.isAtStart)
        assertEquals(w, w.back())
    }

    @Test fun `back undoes next`() {
        val w = GroundingWalkthrough().next().next().next()
        assertEquals(GroundingWalkthrough(2), w.back())
        assertEquals(Sense.See to 3, w.back().current!!.let { it.sense to it.item })
    }

    @Test fun `next at the end stays complete`() {
        val done = GroundingWalkthrough(GroundingWalkthrough.TOTAL)
        assertTrue(done.isComplete)
        assertSame(done, done.next())
    }

    @Test fun `skip sense jumps to the next sense`() {
        val midSee = GroundingWalkthrough(2) // see, item 3
        val skipped = midSee.skipSense()
        assertEquals(Sense.Touch, skipped.current!!.sense)
        assertEquals(1, skipped.current!!.item)

        val taste = GroundingWalkthrough(14)
        assertTrue(taste.skipSense().isComplete)
    }

    @Test fun `skipping every sense takes five taps`() {
        var w = GroundingWalkthrough()
        repeat(5) { w = w.skipSense() }
        assertTrue(w.isComplete)
    }

    @Test fun `restart returns to the first prompt`() {
        val w = GroundingWalkthrough(9).restart()
        assertTrue(w.isAtStart)
        assertEquals(Sense.See, w.current!!.sense)
    }

    @Test fun `progress rises monotonically`() {
        var w = GroundingWalkthrough()
        var last = -1f
        while (!w.isComplete) {
            assertTrue(w.progress > last)
            last = w.progress
            w = w.next()
        }
        assertFalse(GroundingWalkthrough().isComplete)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `out of range index is rejected`() {
        GroundingWalkthrough(16)
    }
}
