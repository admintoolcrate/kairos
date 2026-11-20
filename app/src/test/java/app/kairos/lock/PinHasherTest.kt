package app.kairos.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test fun `right pin verifies, wrong pin does not`() {
        val h = PinHasher.hash("2468")
        assertTrue(PinHasher.verify("2468", h))
        assertFalse(PinHasher.verify("2469", h))
        assertFalse(PinHasher.verify("", h))
    }

    @Test fun `same pin gets different salts`() {
        assertNotEquals(PinHasher.hash("1234").hash, PinHasher.hash("1234").hash)
    }

    @Test fun `pin must be 4 to 8 digits`() {
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertTrue(PinHasher.isValidPin("1234"))
        assertTrue(PinHasher.isValidPin("12345678"))
        assertFalse(PinHasher.isValidPin("123456789"))
    }

    @Test fun `corrupt stored values never verify`() {
        assertFalse(PinHasher.verify("1234", PinHasher.Hashed("", "")))
        assertFalse(PinHasher.verify("1234", PinHasher.Hashed("%%%", "%%%")))
    }

    @Test fun `backoff allows five tries then pauses and grows`() {
        var b = PinBackoff()
        repeat(4) { b = b.failed(0); assertTrue(b.canTry(0)) }
        b = b.failed(0)
        assertFalse(b.canTry(0))
        assertEquals(30_000L, b.waitMillis(0))
        b = b.failed(30_000)
        assertEquals(60_000L, b.waitMillis(30_000))
    }
}
