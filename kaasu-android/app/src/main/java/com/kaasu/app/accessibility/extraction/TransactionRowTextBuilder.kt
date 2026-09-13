package com.kaasu.app.accessibility.extraction

/** Pure result of reconstructing one transaction-history row from its on-screen text fragments. */
data class ReconstructedRow(
    val sentence: String,
    /**
     * [sentence] rewritten into the shape `TransactionParser` already understands
     * ("Paid to X ₹20 on 13 September"). Screen rows put the merchant first with no verb —
     * "JAWAHAR NAGAR 70 FEET RD ₹20 debited 13 September" — which every `MerchantParser` pattern
     * misses, since they all key off a verb. Null when the row had no merchant to anchor on, in
     * which case callers fall back to [sentence].
     */
    val canonicalSentence: String?,
    val amountText: String?,
    val merchantText: String?,
    val dateText: String?,
    val directionHint: String?
)

/**
 * Turns a list of raw text fragments read off one transaction-history row (in on-screen reading
 * order, e.g. `["Paid to Swiggy", "₹245", "28 Aug"]`) into one reconstructed sentence plus the
 * individual fields [com.kaasu.app.accessibility.model.ScrapedTransactionCandidate] carries.
 *
 * Deliberately Android-free and pure — this is the testable seam downstream of each
 * [com.kaasu.app.accessibility.scraper.ScreenScraper]'s Android-dependent node-tree walk, mirroring
 * how the statement-import parsers separated pure text-shape logic from Android-dependent
 * extraction (`XlsxParsing` vs `XlsxTextExtractor`).
 */
object TransactionRowTextBuilder {

    // `[₹Rs]` was a character class, so it matched a bare "R" or "s" — "ARUN STORES 70 FEET RD"
    // parsed as the amount "S 7", and "Bank balances 5 accounts" as "s 5". Must be an alternation,
    // with word boundaries so the letters only count as a currency prefix.
    private val AMOUNT_PATTERN = Regex(
        """(?:₹|\bRs\.?|\bINR)\s?[\d,]+(?:\.\d{1,2})?""",
        RegexOption.IGNORE_CASE
    )

    private val DIRECTION_VERBS = listOf(
        "paid to", "paid", "sent to", "received from", "credited", "debited",
        "refund from", "cashback from"
    )

    // Verbs that mean money arrived. Everything else in DIRECTION_VERBS means it left.
    private val INBOUND_VERBS = setOf("received from", "credited", "refund from", "cashback from")

    /** A fragment counts as the date only if the match covers at least this much of it. */
    private const val DATE_COVERAGE_PERCENT = 60

    private const val MONTHS = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec"

    // The day-then-word branch used to accept any word, so "₹20 debited" read as the date "20
    // debited" and "ARUN STORES 70 FEET RD" as "70 FEET". Anchoring on real month names is what
    // makes it a date pattern rather than a number-next-to-a-word pattern.
    private val DATE_PATTERN = Regex(
        """\b(today|yesterday""" +
            """|\d{1,2}\s+(?:$MONTHS)[a-z]*(\s+\d{4})?""" +
            """|(?:$MONTHS)[a-z]*\s+\d{1,2},?(\s*\d{4})?""" +
            """|\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Builds a [ReconstructedRow] from [fragments], or null if the row carries no parseable
     * amount — without that there is nothing for [com.kaasu.app.notification.parser.TransactionParser]
     * to anchor on downstream, so the row isn't worth turning into a candidate at all.
     */
    fun build(fragments: List<String>): ReconstructedRow? {
        val cleaned = fragments.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return null

        val sentence = cleaned.joinToString(" ")
        if (sentence.isBlank()) return null

        val amountFragment = cleaned.firstOrNull { AMOUNT_PATTERN.containsMatchIn(it) }
        val amountText = amountFragment?.let { AMOUNT_PATTERN.find(it)?.value }
            ?: AMOUNT_PATTERN.find(sentence)?.value
            ?: return null

        // A date fragment has to be mostly date. "ARUN STORES 70 FEET RD" contains "70 FEET", which
        // satisfies the `<day> <month-ish>` branch of DATE_PATTERN and would otherwise steal the
        // merchant's fragment, leaving the real date ("13 September") to be used as the merchant.
        val dateFragment = cleaned.firstOrNull { fragment ->
            val match = DATE_PATTERN.find(fragment)?.value ?: return@firstOrNull false
            match.length * 100 >= fragment.length * DATE_COVERAGE_PERCENT
        }
        val dateText = dateFragment?.let { DATE_PATTERN.find(it)?.value }

        val directionFragment = cleaned.firstOrNull { fragment ->
            DIRECTION_VERBS.any { verb -> fragment.contains(verb, ignoreCase = true) }
        }
        val directionHint = directionFragment?.let { fragment ->
            DIRECTION_VERBS.firstOrNull { verb -> fragment.contains(verb, ignoreCase = true) }
        }

        val merchantText = directionFragment?.let { fragment ->
            val verb = directionHint ?: return@let null
            val idx = fragment.indexOf(verb, ignoreCase = true)
            if (idx < 0) return@let null
            fragment.substring(idx + verb.length).trim().ifBlank { null }
        } ?: cleaned.firstOrNull { it != amountFragment && it != dateFragment && it != directionFragment }

        // "Paid to"/"Received from" are the two shapes MerchantParser and TransactionTypeParser
        // both read cleanly, so canonicalising to one of them lets the existing parser do the work
        // rather than teaching it a fourth input dialect.
        val canonicalSentence = merchantText?.let { merchant ->
            val verb = if (directionHint != null && directionHint in INBOUND_VERBS) "Received from" else "Paid to"
            buildString {
                append(verb).append(' ').append(merchant)
                append(' ').append(amountText)
                if (dateText != null) append(" on ").append(dateText)
            }
        }

        return ReconstructedRow(
            sentence = sentence,
            canonicalSentence = canonicalSentence,
            amountText = amountText,
            merchantText = merchantText,
            dateText = dateText,
            directionHint = directionHint
        )
    }
}
