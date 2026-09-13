package com.kaasu.app.notification.parser

// Extracts the masked last-4 account/card digits from Indian bank notification text.
// Used by the notification pipeline to auto-link transactions to user-registered accounts.
//
// Handles these real-world formats:
//   Union Bank: "A/c *0000 Credited"
//   HDFC/SBI:   "A/c XX1234 debited"   "SBI A/c XXXXXXXX1234"
//   ICICI/Axis: "A/c no. XX9012"       "account ending 7890"
//   Credit card: "Card ending 4321"    "Debit Card XX1234"
object AccountNotificationParser {

    fun extractLastFour(text: String): String? {
        for (pattern in PATTERNS) {
            val result = pattern.find(text)?.groupValues?.get(1)
            if (result != null) return result
        }
        return null
    }

    // A short, human-friendly bank/issuer label found in the text, e.g. "HDFC", "Union Bank".
    // Used to name auto-created accounts. Returns null when no known issuer is mentioned.
    fun extractIssuer(text: String): String? {
        val lower = text.lowercase()
        return ISSUERS.firstOrNull { (key, _) -> lower.contains(key) }?.second
    }

    // True when the text refers to a credit card (used to pick the account type).
    fun isCreditCard(text: String): Boolean = text.contains("credit card", ignoreCase = true)

    // Lowercase keyword → display label. Order matters (more specific first).
    private val ISSUERS = listOf(
        "hdfc" to "HDFC",
        "icici" to "ICICI",
        "state bank" to "SBI",
        "sbi" to "SBI",
        "axis" to "Axis",
        "kotak" to "Kotak",
        "idfc" to "IDFC FIRST",
        "union bank" to "Union Bank",
        "punjab national" to "PNB",
        "pnb" to "PNB",
        "bank of baroda" to "Bank of Baroda",
        "canara" to "Canara",
        "indusind" to "IndusInd",
        "yes bank" to "Yes Bank",
        "rbl" to "RBL",
        "au small" to "AU",
        "federal" to "Federal",
        "bandhan" to "Bandhan",
        "idbi" to "IDBI",
        "indian bank" to "Indian Bank",
    )

    private val PATTERNS = listOf(
        // A/c *0000 / A/c XX1234 / A/c no. ending XX9012 / SBI A/c XXXXXXXX1234
        Regex("""[Aa]/[Cc]\.?\s*(?:[Nn]o\.?\s*(?:ending\s*)?)?[*Xx]{1,}\s*(\d{4})\b"""),
        // "account ending 7890"
        Regex("""[Aa]ccount\s+ending\s+(\d{4})\b"""),
        // "account XXXX7890" (at least 1 masking char required to avoid matching amounts)
        Regex("""[Aa]ccount\s+[*Xx]{1,}\s*(\d{4})\b"""),
        // "Card ending 1234" / "Debit Card XX1234" / "Credit Card ending 4321"
        Regex("""[Cc]ard\s+(?:[Nn]o\.?\s+)?(?:ending\s+)?[*Xx]{0,}\s*(\d{4})\b"""),
        // Generic fallback: at least 2 masking chars immediately before 4 digits ("XXXX1234", "****1234")
        Regex("""[*Xx]{2,}\s*(\d{4})\b"""),
    )
}
