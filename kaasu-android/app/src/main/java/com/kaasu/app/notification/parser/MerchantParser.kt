package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType

object MerchantParser {

    // Called by TransactionParser.parse(); uses TransactionType to decide which regex family to try first
    fun parse(text: String, type: TransactionType): String? {
        return when (type) {
            TransactionType.EXPENSE,
            TransactionType.TRANSFER -> text.firstMatch(
                TO_PATTERN, AT_PATTERN, FVG_PATTERN, TOWARDS_PATTERN,
                BENEFICIARY_CREDITED_PATTERN, TO_OUTCOME_PATTERN, VPA_PATTERN
            )

            // PAID_YOU first: "SENDER paid you Rs.X" names the sender before any "from".
            TransactionType.INCOME -> text.firstMatch(
                PAID_YOU_PATTERN, FROM_PATTERN, FVG_PATTERN, VPA_PATTERN
            )

            // A refund/cashback is still money arriving, so it is named the same way income is.
            // PAID_YOU_PATTERN was missing here, which is why GPay's
            // "NAME paid you Rs.300 Bike repair Refund" resolved to no merchant at all.
            TransactionType.REFUND,
            TransactionType.CASHBACK -> text.firstMatch(
                PAID_YOU_PATTERN, FROM_PATTERN, TO_OUTCOME_PATTERN, VPA_PATTERN
            )

            else -> text.firstMatch(
                TO_PATTERN, AT_PATTERN, FVG_PATTERN, TOWARDS_PATTERN,
                BENEFICIARY_CREDITED_PATTERN, TO_OUTCOME_PATTERN, FROM_PATTERN, VPA_PATTERN
            )
        }
    }

    // Tries each pattern in order and returns the first that yields a usable merchant. Ordering is
    // the precedence rule: narrow, format-specific patterns must come before the loose fallbacks,
    // since VPA_PATTERN in particular will match a handle almost anywhere in the text.
    private fun String.firstMatch(vararg patterns: Regex): String? {
        for (pattern in patterns) {
            pattern.find(this)?.groupValues?.get(1)?.clean()?.let { return it }
        }
        return null
    }

    private fun String.clean(): String? {
        // Strip @domain suffix from VPA handles (e.g. "swiggy@icici" → "swiggy")
        val withoutVpaDomain = if (contains('@')) substringBefore('@') else this
        val result = withoutVpaDomain
            .trim()
            .trim('.', ',', '!', '?', ':', ';', ')', '(', '-', '_', '"', '\'', ' ')
            .replace(WHITESPACE, " ")
            .take(50)
            .trim()
        if (result.length < 2) return null
        // Reject junk: bare stop-words ("your", "account", "self"…) and tokens with no letters
        if (result.lowercase() in MERCHANT_STOPWORDS) return null
        if (result.none { it.isLetter() }) return null
        return result
    }

    private val WHITESPACE = Regex("""\s+""")

    // Words that are never a real merchant on their own — usually the parser overshot into
    // "...to your account" / "...to self" style phrasing.
    private val MERCHANT_STOPWORDS = setOf(
        "your", "a/c", "ac", "account", "savings", "self", "upi", "bank",
        "the", "you", "to", "from", "via", "card", "wallet"
    )

    // Words that signal the merchant name has ended; also barred from being absorbed mid-name.
    // Used both as a per-word guard (so "Swiggy for 5% offer" stops at "Swiggy") and as the
    // trailing terminator. The capture also stops at punctuation, "(", or a digit-led token.
    private const val STOP = """via|using|through|from|to|on|for|with|is|has|ref|upi|rs|inr|and|your|the"""

    // "paid to X", "sent to X", "transferred to X"
    private val TO_PATTERN = Regex(
        """(?:paid?\s+to|sent?\s+to|transfer(?:red)?\s+to)\s+(?!(?:$STOP)\b)([A-Za-z0-9][\w@._-]*(?:\s+(?!(?:$STOP)\b)[A-Za-z0-9][\w@._-]*){0,2})(?=\s+(?:$STOP)\b|\s*[.,!?(]|\s+\d|\s*$)""",
        RegexOption.IGNORE_CASE
    )

    // "at Swiggy on DATE", "at Zomato", "at McDonald's"
    // Allows 1–3 words; negative lookahead on the first and each extra word prevents stop words
    // from being absorbed ("at Zepto on 24-04-26 via UPI" → "Zepto", not "Zepto on 24-04-26").
    private val AT_PATTERN = Regex(
        """(?<!\w)at\s+(?!(?:$STOP)\b)([A-Za-z][\w'.-]*(?:\s+(?!(?:$STOP)\b)[A-Za-z][\w'.-]*){0,2})(?=\s+(?:$STOP)\b|\s*[.,!?(]|\s+\d|\s*$)""",
        RegexOption.IGNORE_CASE
    )

    // "received from Priya Kumar", "from sender@ybl"
    private val FROM_PATTERN = Regex(
        """(?:received\s+from|from)\s+(?!(?:$STOP)\b)([A-Za-z0-9][\w@._-]*(?:\s+(?!(?:$STOP)\b)[A-Za-z0-9][\w@._-]*){0,2})(?=\s+(?:$STOP)\b|\s*[.,!?(]|\s+\d|\s*$)""",
        RegexOption.IGNORE_CASE
    )

    // GPay income format: "MEENAKSHI . paid you ₹1.00 for lunch"
    // The sender name is everything before "paid you" / "sent you", separated by optional ". "
    private val PAID_YOU_PATTERN = Regex(
        """^([\w\s.'-]{1,40}?)\s*\.?\s*(?:paid|sent)\s+you\b""",
        RegexOption.IGNORE_CASE
    )

    // VPA handle: merchant@icici, name@ybl, name@okaxis
    private val VPA_PATTERN = Regex(
        """([A-Za-z0-9][\w._-]{0,48})@[A-Za-z]{2,}"""
    )

    // Union Bank: "A/c *0000 Debited Rs:115.00 ... Fvg: ARUN TRAD Avl Bal Rs:1391.54"
    // "Fvg" (favouring) names the beneficiary, and the field is reliably terminated by the
    // running balance. Delimiter-to-delimiter capture rather than a word-count guess, because
    // these names are free-form and truncated by the bank ("HP Pay D", "D ASHOK", "NANDHA K").
    private val FVG_PATTERN = Regex(
        """\bfvg:?\s+([A-Za-z][^;]*?)\s+av(?:l|bl)\s+bal\b""",
        RegexOption.IGNORE_CASE
    )

    // IDFC mandate debit: "debited with Rs 300.00 towards EXAMPLE SECURITIES LIMITED SI for the
    // UPI Mandate on 28/11/2025". The merchant sits between "towards" and the next clause.
    private val TOWARDS_PATTERN = Regex(
        """\btowards\s+([A-Za-z][^;.,]*?)(?=\s+(?:for|on|via|through|vide)\b|\s*[.,;]|\s*$)""",
        RegexOption.IGNORE_CASE
    )

    // IDFC P2P debit: "Your A/c XX1234 debited by Rs. 5.00 on 28/11/25; EXAMPLE SECURITIES LI
    // credited." Here the beneficiary PRECEDES the verb, so the "to X" patterns never see it.
    // Anchored on the clause separator so it cannot reach back into the debited-account phrase.
    private val BENEFICIARY_CREDITED_PATTERN = Regex(
        """[;,]\s*([A-Za-z][^;,]*?)\s+credited\b""",
        RegexOption.IGNORE_CASE
    )

    // GPay autopay/mandate: "Payment for Autopay of Rs.835.44 to Hostinger was successful",
    // "Payment of Rs.872.10 to Amazon Pay is scheduled for Jul 1". Plain "to X" — no "paid"/
    // "sent" verb in front of it — so TO_PATTERN cannot match. Requires a following outcome
    // verb, which keeps it from firing on arbitrary "to" occurrences.
    private val TO_OUTCOME_PATTERN = Regex(
        """\bto\s+([A-Za-z][^;.,]*?)\s+(?:was|is|has\s+been)\s+(?:successful|scheduled|completed|processed|debited|credited)\b""",
        RegexOption.IGNORE_CASE
    )
}
