package app.kairos.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvelopeTest {
    // Fewer iterations keep the test fast; the format records the count used.
    private val fast = 1_000
    private val plain = """{"format":"kairos","version":1,"note":"private words"}"""

    @Test fun `seal and open with the right passphrase`() {
        val sealed = Envelope.write(plain, "correct horse".toCharArray(), fast)
        assertTrue(Envelope.isSealed(sealed))
        assertFalse(sealed.contains("private words"))
        assertEquals(plain, Envelope.read(sealed, "correct horse".toCharArray()))
    }

    @Test(expected = WrongPassphraseException::class)
    fun `wrong passphrase fails cleanly`() {
        Envelope.read(Envelope.write(plain, "one".toCharArray(), fast), "two".toCharArray())
    }

    @Test(expected = PassphraseNeededException::class)
    fun `sealed file without passphrase asks for one`() {
        Envelope.read(Envelope.write(plain, "one".toCharArray(), fast), null)
    }

    @Test fun `no passphrase means plain JSON`() {
        assertEquals(plain, Envelope.write(plain, null))
        assertEquals(plain, Envelope.write(plain, CharArray(0)))
        assertFalse(Envelope.isSealed(plain))
        assertEquals(plain, Envelope.read(plain, "ignored".toCharArray()))
    }

    @Test fun `each seal uses a fresh salt and iv`() {
        val a = Envelope.write(plain, "p".toCharArray(), fast)
        val b = Envelope.write(plain, "p".toCharArray(), fast)
        assertNotEquals(a, b)
    }

    @Test fun `default iterations open too`() {
        val sealed = Envelope.write(plain, "slow and steady".toCharArray())
        assertEquals(plain, Envelope.read(sealed, "slow and steady".toCharArray()))
    }
}
