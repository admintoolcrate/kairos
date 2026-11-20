package app.kairos.lock

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PBKDF2-SHA256 for the app PIN. The PIN itself is never stored. */
object PinHasher {
    const val ITERATIONS = 120_000
    val PIN_LENGTH = 4..8

    data class Hashed(val hash: String, val salt: String)

    fun isValidPin(pin: String) = pin.length in PIN_LENGTH && pin.all { it.isDigit() }

    fun hash(pin: String, salt: ByteArray = randomSalt()): Hashed {
        require(isValidPin(pin)) { "PIN must be ${PIN_LENGTH.first}-${PIN_LENGTH.last} digits" }
        return Hashed(b64(derive(pin, salt)), b64(salt))
    }

    fun verify(pin: String, hashed: Hashed): Boolean {
        if (!isValidPin(pin) || hashed.hash.isEmpty() || hashed.salt.isEmpty()) return false
        val salt = runCatching { Base64.getDecoder().decode(hashed.salt) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(hashed.hash) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derive(pin, salt), expected)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun randomSalt() = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private fun b64(b: ByteArray) = Base64.getEncoder().encodeToString(b)
}

/**
 * After a few wrong PINs, a short, growing pause. No lock-out, no data wiped,
 * no alarming wording: just "try again in a moment".
 */
data class PinBackoff(val failures: Int = 0, val until: Long = 0) {
    fun canTry(now: Long) = now >= until
    fun waitMillis(now: Long) = (until - now).coerceAtLeast(0)

    fun failed(now: Long): PinBackoff {
        val f = failures + 1
        val wait = if (f < FREE_TRIES) 0L else (30_000L shl (f - FREE_TRIES).coerceAtMost(4))
        return PinBackoff(f, now + wait)
    }

    companion object {
        const val FREE_TRIES = 5
    }
}
