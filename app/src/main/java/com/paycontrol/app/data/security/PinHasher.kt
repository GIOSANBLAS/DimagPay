package com.paycontrol.app.data.security

import java.security.MessageDigest
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de PIN con PBKDF2-HMAC-SHA256.
 * Formato: `v2$<iterations>$<hashHex>` (la sal vive aparte en preferencias).
 * Verifica también hashes legacy SHA-256(pin+salt).
 */
object PinHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val V2_PREFIX = "v2$"

    fun generateSalt(): String = UUID.randomUUID().toString()

    fun hash(pin: String, salt: String): String {
        val derived = pbkdf2(pin, salt, ITERATIONS)
        return "$V2_PREFIX$ITERATIONS$${derived.toHex()}"
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean {
        if (expectedHash.startsWith(V2_PREFIX)) {
            val parts = expectedHash.split('$')
            if (parts.size != 3) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val expected = parts[2].fromHex() ?: return false
            val actual = pbkdf2(pin, salt, iterations)
            return MessageDigest.isEqual(actual, expected)
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((pin + salt).toByteArray(Charsets.UTF_8))
        val legacy = bytes.toHex()
        return MessageDigest.isEqual(
            legacy.toByteArray(Charsets.UTF_8),
            expectedHash.lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    private fun pbkdf2(pin: String, salt: String, iterations: Int): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            iterations.coerceAtLeast(10_000),
            KEY_LENGTH_BITS
        )
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { b -> "%02x".format(b) }

    private fun String.fromHex(): ByteArray? = runCatching {
        if (length % 2 != 0) return null
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }.getOrNull()
}
