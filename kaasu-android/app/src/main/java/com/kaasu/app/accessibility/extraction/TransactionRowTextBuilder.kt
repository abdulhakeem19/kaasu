package com.kaasu.app.accessibility.extraction

/** Pure result of reconstructing one transaction-history row from its on-screen text fragments. */
data class ReconstructedRow(
    val sentence: String,
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

    private val AMOUNT_PATTERN = Regex("""[₹Rs]\.?\s?[\d,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)

    private val DIRECTION_VERBS = listOf(
        "paid to", "paid", "sent to", "received from", "credited", "debited",
        "refund from", "cashback from"
    )

    private val DATE_PATTERN = Regex(
        """\b(today|yesterday|\d{1,2}\s+[A-Za-z]{3,}(\s+\d{4})?|[A-Za-z]{3,}\s+\d{1,2},?(\s*\d{4})?|\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""",
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

        val dateFragment = cleaned.firstOrNull { DATE_PATTERN.containsMatchIn(it) }
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

        return ReconstructedRow(
            sentence = sentence,
            amountText = amountText,
            merchantText = merchantText,
            dateText = dateText,
            directionHint = directionHint
        )
    }
}
