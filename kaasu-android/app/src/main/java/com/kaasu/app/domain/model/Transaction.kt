package com.kaasu.app.domain.model

data class Transaction(
    val id: Long = 0,
    val amountInPaise: Long,
    val currency: String = "INR",
    val type: TransactionType,
    val merchantName: String?,
    val categoryId: Long?,
    val sourceAppPackage: String?,
    val sourceAppName: String?,
    val paymentMode: String?,
    // rawText is deliberately excluded — it never surfaces above the data layer
    val rawTextHash: String?,
    val confidenceScore: Int = 0,
    val transactionTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isManual: Boolean = false,
    val isTransfer: Boolean = false,
    val isRefund: Boolean = false,
    val isIgnored: Boolean = false,
    val note: String?,
    val accountId: Long? = null,
    val isRecurring: Boolean = false,
    val parentId: Long? = null,
    val isDuplicate: Boolean = false,
    // Set on both legs of a movement between the owner's own accounts. See TransferRole.
    val transferGroupId: String? = null,
    val transferRole: TransferRole? = null,
    // The account at the other end — for showing "Savings → SBI Card". Never summed.
    val counterpartAccountId: Long? = null
)
