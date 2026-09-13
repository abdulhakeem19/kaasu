package com.kaasu.app.statement.model

/**
 * Preview/result summary shown on the import screen: how many lines the parser found in the
 * statement, how many are new (not already tracked) and would be inserted, and how many were
 * skipped as already-tracked duplicates. [newItems] backs the scrollable preview list before commit.
 */
data class ImportResult(
    val bankDisplayName: String,
    val totalFound: Int,
    val newItems: List<StatementLineItem>,
    val duplicateCount: Int,
) {
    val newCount: Int get() = newItems.size
}
