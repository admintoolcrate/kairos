package app.kairos.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPassphraseException : Exception("That passphrase didn't open this backup.")
class PassphraseNeededException : Exception("This backup is protected with a passphrase.")

/**
 * A backup file is either plain Kairos JSON, or that JSON sealed with
 * AES-256-GCM under a key derived from the user's passphrase (PBKDF2-SHA256),
 * so nobody, Google included, can read it without the passphrase.
 * (Same scheme as Stagira's sync envelope.)
 */
object Envelope {
    const val FORMAT = "kairos-sealed"
    const val ITERATIONS = 210_000
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private val random = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Sealed(
        val format: String = FORMAT,
        val version: Int = 1,
        val cipher: String = "AES-256-GCM",
        val kdf: String = "PBKDF2WithHmacSHA256",
        val iterations: Int,
        val salt: String,
        val iv: String,
        val data: String,
    )

    fun isSealed(text: String): Boolean = runCatching {
        json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.content == FORMAT
    }.getOrDefault(false)

    /** Seals [plain] when a non-empty passphrase is given; otherwise returns it as is. */
    fun write(plain: String, passphrase: CharArray?, iterations: Int = ITERATIONS): String {
        if (passphrase == null || passphrase.isEmpty()) return plain
        val salt = bytes(16)
        val iv = bytes(12)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(passphrase, salt, iterations), "AES"), GCMParameterSpec(128, iv))
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        return json.encodeToString(Sealed.serializer(), Sealed(
            iterations = iterations, salt = b64.encodeToString(salt), iv = b64.encodeToString(iv), data = b64.encodeToString(data),
        ))
    }

    /** Opens a sealed file, or returns plain text unchanged. */
    fun read(text: String, passphrase: CharArray?): String {
        if (!isSealed(text)) return text
        if (passphrase == null || passphrase.isEmpty()) throw PassphraseNeededException()
        val s = json.decodeFromString(Sealed.serializer(), text)
        val d = Base64.getDecoder()
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derive(passphrase, d.decode(s.salt), s.iterations), "AES"), GCMParameterSpec(128, d.decode(s.iv)))
        return try {
            cipher.doFinal(d.decode(s.data)).toString(Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            throw WrongPassphraseException()
        }
    }

    private fun derive(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun bytes(n: Int) = ByteArray(n).also { random.nextBytes(it) }
}
