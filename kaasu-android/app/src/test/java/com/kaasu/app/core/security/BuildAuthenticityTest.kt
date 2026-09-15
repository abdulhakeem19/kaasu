package com.kaasu.app.core.security

import com.kaasu.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fingerprint the app claims must be the one the release is actually signed with, or the whole
 * check says "not official" about every genuine install.
 */
class BuildAuthenticityTest {

    /**
     * The certificate of the release keystore, as `apksigner verify --print-certs` reports it for
     * every published APK. Duplicated here on purpose: if someone edits the build file's value, one
     * of the two has to be wrong, and this is where that gets noticed rather than on a user's phone.
     */
    private val releaseCertSha256 =
        "a286824792e66616ed51eaad4a6bba90b619bae770c51b323f7c51b36e5141c9"

    @Test fun `the expected fingerprint matches the release signing certificate`() {
        assertEquals(releaseCertSha256, BuildConfig.EXPECTED_SIGNING_SHA256)
    }

    @Test fun `the fingerprint is a full SHA-256 in lowercase hex`() {
        val value = BuildConfig.EXPECTED_SIGNING_SHA256
        assertEquals("64 hex characters", 64, value.length)
        assertTrue("lowercase hex only", value.all { it in "0123456789abcdef" })
    }

    @Test fun `it is not blank, which would disable the check silently`() {
        assertNotNull(BuildAuthenticity.expectedFingerprint())
        assertTrue(BuildAuthenticity.expectedFingerprint()!!.isNotBlank())
    }

    // ── Display formatting ────────────────────────────────────────────────────

    @Test fun `display format groups bytes the way certificate tools print them`() {
        // So it can be read against apksigner's output without transcribing character by character.
        assertEquals("AB:CD:EF:12", BuildAuthenticity.formatForDisplay("abcdef12"))
    }

    @Test fun `the release fingerprint formats to 32 colon-separated bytes`() {
        val shown = BuildAuthenticity.formatForDisplay(releaseCertSha256)
        assertEquals(32, shown.split(":").size)
        assertTrue(shown.startsWith("A2:86:82:47"))
    }

    @Test fun `an unreadable signature says so rather than showing an empty box`() {
        assertEquals("unavailable", BuildAuthenticity.formatForDisplay(null))
    }
}
