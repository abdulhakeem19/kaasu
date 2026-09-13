package com.kaasu.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 hashing for the local app-lock PIN. The PIN never leaves the device; this just
 * avoids storing it in plain text in DataStore.
 */
object PinHasher {

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val out = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return out.toHex()
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        hash(pin, salt) == expectedHash

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
