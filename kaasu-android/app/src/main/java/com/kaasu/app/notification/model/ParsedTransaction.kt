package com.kaasu.app.notification.model

import com.kaasu.app.domain.model.TransactionType

data class ParsedTransaction(
    val amountInPaise: Long,
    val currency: String = "INR",
    val type: TransactionType,
    val merchantName: String?,
    val sourceAppPackage: String,
    val sourceAppName: String?,
    val rawText: String,
    val confidenceScore: Int,
    val transactionTime: Long,
    val isRecurring: Boolean = false,
    // Free-text note the payer typed in GPay/PhonePe; null when they typed none.
    val note: String? = null
)
