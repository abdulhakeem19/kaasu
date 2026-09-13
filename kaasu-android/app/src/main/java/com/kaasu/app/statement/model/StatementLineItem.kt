package com.kaasu.app.statement.model

import com.kaasu.app.domain.model.TransactionType
import java.time.LocalDate

/**
 * One parsed line item from a bank/card statement (CSV or PDF). Statements carry a calendar
 * date (not a timestamp) and structured line items rather than narrative sentences, so this is
 * a purpose-built model distinct from [com.kaasu.app.notification.model.ParsedTransaction] —
 * see StatementImportManager for why this pipeline doesn't reuse TransactionParser/DuplicateChecker.
 */
data class StatementLineItem(
    val date: LocalDate,
    val amountInPaise: Long,
    val description: String,
    val direction: TransactionType,
    // The original statement line/row text, used for the Tier 1 exact-hash dedup check and
    // stored as the transaction's rawText/rawTextHash on commit (same convention as capture).
    val rawLineText: String,
    // The UPI transaction reference number, when the source line carries one (IDFC XLSX UPI/DR
    // and UPI/CR rows, every GPay PDF row). Powers the Tier 1.5 cross-source dedup check in
    // StatementImportManager — the same real-world payment often shows up in more than one
    // statement (and in bank SMS/notification rawText) all embedding this same reference number.
    // Parsers with no such reference (CSV formats built before this field existed, and IDFC rows
    // like BLKIFT/IFT-OPT/interest credit that aren't UPI transactions) just leave it null.
    val upiTransactionId: String? = null,
)
