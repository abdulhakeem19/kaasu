package com.kaasu.app.core.bank

import androidx.compose.ui.graphics.Color

/**
 * Visual identity for the banks and payment apps Kaasu captures from — a brand colour and a short
 * monogram, resolved from whatever identifier a transaction happens to carry.
 *
 * Accounts previously rendered as a letter on a colour picked by hashing the account's last four
 * digits, so the same bank could appear in a different colour on every card and two unrelated banks
 * could collide on one. Identity here is keyed on the bank itself, so it is stable and consistent
 * everywhere the bank appears.
 *
 * **These are hand-matched approximations of each brand's colour, not official brand assets.** No
 * third-party logo images are bundled: a monogram on the brand colour gives the same at-a-glance
 * recognition without shipping trademarked artwork. If real logos are wanted later, only
 * [BankIdentity] needs an extra field — nothing at the call sites changes.
 */
data class BankIdentity(
    val key: String,
    val displayName: String,
    val monogram: String,
    val color: Color,
)

object BankRegistry {

    private val UNKNOWN = BankIdentity("unknown", "Bank", "•", Color(0xFF6B7A72))

    private val BANKS = listOf(
        BankIdentity("idfc",      "IDFC FIRST",   "IF", Color(0xFF9C1D26)),
        BankIdentity("sbi",       "SBI",          "SB", Color(0xFF22409A)),
        BankIdentity("union",     "Union Bank",   "UB", Color(0xFFCE1126)),
        BankIdentity("hdfc",      "HDFC",         "HD", Color(0xFF004C8F)),
        BankIdentity("icici",     "ICICI",        "IC", Color(0xFFAE282E)),
        BankIdentity("axis",      "Axis",         "AX", Color(0xFF97144D)),
        BankIdentity("kotak",     "Kotak",        "KO", Color(0xFFED1C24)),
        BankIdentity("indusind",  "IndusInd",     "IN", Color(0xFF8C1D40)),
        BankIdentity("canara",    "Canara",       "CB", Color(0xFF00548F)),
        BankIdentity("bob",       "Bank of Baroda", "BB", Color(0xFFF15A22)),
        BankIdentity("pnb",       "PNB",          "PN", Color(0xFFA5842C)),
        BankIdentity("yes",       "YES Bank",     "YB", Color(0xFF00457C)),
        BankIdentity("federal",   "Federal Bank", "FB", Color(0xFF00579C)),
        BankIdentity("gpay",      "Google Pay",   "GP", Color(0xFF1A73E8)),
        BankIdentity("phonepe",   "PhonePe",      "PP", Color(0xFF5F259F)),
        BankIdentity("paytm",     "Paytm",        "PT", Color(0xFF00BAF2)),
        BankIdentity("amazonpay", "Amazon Pay",   "AP", Color(0xFFFF9900)),
        BankIdentity("cred",      "CRED",         "CR", Color(0xFF1A1A1A)),
        BankIdentity("slice",     "slice",        "SL", Color(0xFF6C2BD9)),
    )

    private val byKey = BANKS.associateBy { it.key }

    // Fragments that appear inside DLT sender headers ("JM-IDFCFB-S"), app package names and
    // account display names alike, so one table serves all three lookups. Order matters only where
    // one fragment contains another, which is why these are matched longest-first.
    private val FRAGMENTS: List<Pair<String, String>> = listOf(
        "idfcfb" to "idfc", "idfcfirst" to "idfc", "idfc" to "idfc",
        "sbicrd" to "sbi", "sbicgv" to "sbi", "sbicard" to "sbi", "sbiinb" to "sbi",
        "lotusintouch" to "sbi", "sbi" to "sbi",
        "unionb" to "union", "unionbank" to "union", "union" to "union",
        "hdfcbk" to "hdfc", "hdfc" to "hdfc",
        "icicib" to "icici", "icici" to "icici", "csam" to "icici",
        "axisbk" to "axis", "axis" to "axis",
        "kotakb" to "kotak", "kotak" to "kotak",
        "indusb" to "indusind", "indusind" to "indusind",
        "canbnk" to "canara", "canara" to "canara",
        "barodab" to "bob", "baroda" to "bob",
        "pnbsms" to "pnb", "punjab" to "pnb",
        "yesbnk" to "yes",
        "federal" to "federal", "fedbnk" to "federal",
        "paisa" to "gpay", "googlepay" to "gpay", "gpay" to "gpay",
        "phonepe" to "phonepe",
        "paytmb" to "paytm", "one97" to "paytm", "paytm" to "paytm",
        "amazon" to "amazonpay",
        "dreamplug" to "cred", "cred" to "cred",
        "slicepay" to "slice",
    ).sortedByDescending { it.first.length }

    /**
     * Resolves a bank from any identifier a transaction carries: an SMS sender header
     * ("sms:JM-IDFCFB-S"), an app package ("com.idfcfirstbank.optimus"), or an account's display
     * name ("IDFC FIRST Card"). Returns null when nothing matches, so callers can decide whether to
     * fall back or to leave the slot empty.
     */
    fun resolve(vararg identifiers: String?): BankIdentity? {
        for (raw in identifiers) {
            val needle = raw?.lowercase()?.filter { it.isLetterOrDigit() } ?: continue
            if (needle.isEmpty()) continue
            for ((fragment, key) in FRAGMENTS) {
                if (needle.contains(fragment)) return byKey[key]
            }
        }
        return null
    }

    /** Same as [resolve] but never null — for slots that must always render something. */
    fun resolveOrUnknown(vararg identifiers: String?): BankIdentity =
        resolve(*identifiers) ?: UNKNOWN

    /**
     * Monogram fallback for an account that matched no known bank: its own initials, so a
     * hand-named account ("Cash Wallet") still reads as itself rather than as a generic dot.
     */
    fun monogramFor(displayName: String): String =
        displayName.split(' ', '-', '_')
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "•" }
}
