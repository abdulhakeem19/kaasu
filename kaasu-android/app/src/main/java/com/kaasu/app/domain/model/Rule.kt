package com.kaasu.app.domain.model

data class Rule(
    val id: Long = 0,
    val name: String?,
    val matchText: String,
    val matchType: MatchType,
    val categoryId: Long?,
    val transactionType: TransactionType?,
    val sourceAppPackage: String?,
    val priority: Int = 0,
    val isSystem: Boolean = false,
    val isActive: Boolean = true
)
