package com.paycontrol.app.data.security

import java.security.MessageDigest
import java.util.UUID

/**
 * Hash de PIN con sal (SHA-256). Nunca persistir el PIN en texto plano.
 */
object PinHasher {

    fun generateSalt(): String = UUID.randomUUID().toString()

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((pin + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        hash(pin, salt).equals(expectedHash, ignoreCase = true)
}
