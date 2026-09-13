package com.kaasu.app.statement.parser

import com.kaasu.app.statement.model.StatementLineItem

/**
 * One (bank, format) statement parser — the statement-import analogue of the per-bank
 * hardcoded matching in [com.kaasu.app.notification.parser.AccountNotificationParser]. The
 * owner's bank set is small and fixed, so a hand-written parser per format is both less code
 * and more accurate than a generic column-mapping heuristic.
 *
 * Implementations fingerprint themselves via [canParse] against the file's own header/content —
 * never by filename — so [com.kaasu.app.statement.StatementParserRegistry] can dispatch on the
 * first match.
 */
interface StatementParser {
    val id: String
    val displayName: String

    // Called with a small sample of the extracted text (the file's opening chars) to decide
    // whether this parser recognizes the format. Must be cheap — no full parse.
    fun canParse(sample: String): Boolean

    // Full parse of the extracted text (the whole CSV file, or the whole PdfBox-extracted PDF
    // text) into line items. Malformed/unparseable rows are skipped rather than throwing.
    fun parse(fullText: String): List<StatementLineItem>
}
