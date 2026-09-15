package com.kaasu.app.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.kaasu.app.BuildConfig
import java.security.MessageDigest

/**
 * Answers "is this the official Kaasu, or something wearing its name?"
 *
 * Kaasu is not on the Play Store, so there is no store listing vouching for a download. What does
 * vouch for it is the **signing certificate**: Android refuses to install an update whose signature
 * differs from the installed app, and only the release keystore can produce this one. Anyone can
 * rebuild the source, but nobody else can sign it with this key.
 *
 * ## What this is not
 *
 * A determined attacker shipping a repackaged APK can also patch out this check — the source is
 * public, so the code doing the asking is as editable as anything else. This is therefore a way to
 * tell *which build you are running*, not a guarantee that the running build is honest. It catches
 * an unofficial or accidentally-sideloaded build; it cannot catch a build rewritten to lie.
 *
 * The verification that cannot be patched out happens **before** installing, off the device:
 *
 *     apksigner verify --print-certs kaasu-<version>.apk
 *     shasum -a 256 kaasu-<version>.apk   # compare with the .sha256 in the GitHub release
 *
 * which is why [expectedFingerprint] is surfaced in the UI — so the fingerprint on the phone can be
 * read against the one published in the README, by someone who trusts neither blindly.
 */
object BuildAuthenticity {

    /** SHA-256 of the release signing certificate, as `apksigner verify --print-certs` prints it. */
    private val EXPECTED: String? = BuildConfig.EXPECTED_SIGNING_SHA256.takeIf { it.isNotBlank() }

    sealed interface Status {
        /** Signed by the official release key. */
        data object Official : Status

        /** A developer build. Expected on a machine that built it; unexpected on a phone. */
        data object DebugBuild : Status

        /** Signed by some other key — rebuilt by someone else, or repackaged. */
        data class Unrecognised(val fingerprint: String?) : Status

        /** The signature could not be read at all, which is itself not normal. */
        data object Unknown : Status
    }

    fun status(context: Context): Status {
        if (BuildConfig.DEBUG) return Status.DebugBuild
        val actual = signingFingerprint(context) ?: return Status.Unknown
        val expected = EXPECTED ?: return Status.Unrecognised(actual)
        return if (actual.equals(expected, ignoreCase = true)) Status.Official
        else Status.Unrecognised(actual)
    }

    /** The running app's signing certificate fingerprint, lowercase hex, or null if unreadable. */
    fun signingFingerprint(context: Context): String? = runCatching {
        val pm = context.packageManager
        val pkg = context.packageName

        val certificates: Array<android.content.pm.Signature> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                val signing = info.signingInfo ?: return null
                // A rotated key reports its history; the current signer is what Android enforces.
                if (signing.hasMultipleSigners()) signing.apkContentsSigners
                else signing.signingCertificateHistory
            } else {
                // API 26–27 predate signing-certificate rotation and expose only the raw signature.
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures ?: return null
            }

        certificates.firstOrNull()?.toByteArray()?.let { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
        }
    }.getOrNull()

    /** The fingerprint an official build should have, for comparing against the published one. */
    fun expectedFingerprint(): String? = EXPECTED

    /** Grouped into colon-separated bytes, the way certificate tooling prints them. */
    fun formatForDisplay(fingerprint: String?): String =
        fingerprint?.chunked(2)?.joinToString(":")?.uppercase() ?: "unavailable"

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
