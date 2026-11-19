package app.kairos.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrisisLinesTest {
    @Test fun `known region gives its line and emergency number`() {
        val us = CrisisLines.resolve("us")
        assertEquals("988", us.line!!.number)
        assertEquals("911", us.emergencyNumber)
        assertTrue(us.known)
        assertFalse(us.isOverride)

        assertEquals("999", CrisisLines.resolve("GB").emergencyNumber)
        assertEquals("000", CrisisLines.resolve("AU").emergencyNumber)
    }

    @Test fun `user override always wins`() {
        val mine = CrisisLine("My therapist's out-of-hours line", "+44 20 7946 0000")
        val r = CrisisLines.resolve("US", mine)
        assertEquals(mine, r.line)
        assertTrue(r.isOverride)
        assertEquals("911", r.emergencyNumber)
    }

    @Test fun `blank override is ignored`() {
        assertEquals("988", CrisisLines.resolve("US", CrisisLine("x", "  ")).line!!.number)
    }

    @Test fun `unknown region falls back to 112 and asks the user`() {
        val r = CrisisLines.resolve("ZZ")
        assertNull(r.line)
        assertFalse(r.known)
        assertEquals("112", r.emergencyNumber)
        assertFalse(CrisisLines.resolve(null).known)
        assertNull(CrisisLines.resolve("USA").region)
    }

    @Test fun `override works for unknown regions`() {
        val r = CrisisLines.resolve(null, CrisisLine("Friend", "123"))
        assertTrue(r.known)
    }

    @Test fun `every table entry has a dialable number`() {
        CrisisLines.table.forEach { (code, region) ->
            assertEquals(2, code.length)
            assertTrue(code, CrisisLines.dialable(region.emergency).isNotEmpty())
            assertTrue(code, CrisisLines.dialable(region.line!!.number).isNotEmpty())
        }
    }

    @Test fun `dialable strips spaces and keeps a leading plus`() {
        assertEquals("116123", CrisisLines.dialable("116 123"))
        assertEquals("+442079460000", CrisisLines.dialable(" +44 (20) 7946-0000"))
    }
}
