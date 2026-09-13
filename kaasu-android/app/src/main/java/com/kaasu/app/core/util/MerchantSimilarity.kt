package com.kaasu.app.core.util

// Merchant-name similarity check shared by every dedup path in the app: DuplicateChecker (live
// notification/SMS capture, near-simultaneous cross-source echoes) and StatementImportManager
// (statement-import Tier 2 dedup, same-day cross-source matches). Extracted from
// DuplicateChecker so both can reuse the exact same matching rule without forking the logic.
object MerchantSimilarity {

    // Substring match (case-insensitive) so e.g. "Swiggy" and "Swiggy Instamart" are treated as
    // the same merchant. Both-null counts as a match (e.g. two bank-SMS confirmations with no
    // merchant name); exactly one null is not a match.
    fun areSimilar(a: String?, b: String?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        val aL = a.lowercase().trim()
        val bL = b.lowercase().trim()
        return aL == bL || aL.contains(bL) || bL.contains(aL)
    }
}
