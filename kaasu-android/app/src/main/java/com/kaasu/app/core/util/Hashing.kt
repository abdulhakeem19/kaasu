package com.kaasu.app.core.util

import java.security.MessageDigest

// Shared hashing helpers. Moved out of KaasuNotificationListenerService so both the
// notification and SMS capture paths (and the SMS backfill worker's idempotency check)
// can produce/compare the same rawTextHash.
object Hashing {

    // Produces rawTextHash stored in the DB — used by DuplicateChecker queries (not the rawText
    // itself, which is privacy-sensitive). Same algorithm as before the extraction: do not change
    // it, or existing rawTextHash values in the DB stop matching newly computed hashes.
    fun sha256Prefix(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
