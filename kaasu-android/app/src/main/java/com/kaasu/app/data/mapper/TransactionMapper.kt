package com.kaasu.app.data.mapper

import com.kaasu.app.core.database.entity.TransactionEntity
import com.kaasu.app.domain.model.Transaction
import com.kaasu.app.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountInPaise = amountInPaise,
    currency = currency,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.UNKNOWN),
    merchantName = merchantName,
    categoryId = categoryId,
    sourceAppPackage = sourceAppPackage,
    sourceAppName = sourceAppName,
    paymentMode = paymentMode,
    rawTextHash = rawTextHash,
    confidenceScore = confidenceScore,
    transactionTime = transactionTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isManual = isManual,
    isTransfer = isTransfer,
    isRefund = isRefund,
    isIgnored = isIgnored,
    note = note,
    accountId = accountId,
    isRecurring = isRecurring,
    parentId = parentId,
    isDuplicate = isDuplicate
)

fun Transaction.toEntity(rawText: String? = null): TransactionEntity = TransactionEntity(
    id = id,
    amountInPaise = amountInPaise,
    currency = currency,
    type = type.name,
    merchantName = merchantName,
    categoryId = categoryId,
    sourceAppPackage = sourceAppPackage,
    sourceAppName = sourceAppName,
    paymentMode = paymentMode,
    rawText = rawText,
    rawTextHash = rawTextHash,
    confidenceScore = confidenceScore,
    transactionTime = transactionTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isManual = isManual,
    isTransfer = isTransfer,
    isRefund = isRefund,
    isIgnored = isIgnored,
    note = note,
    accountId = accountId,
    isRecurring = isRecurring,
    parentId = parentId,
    isDuplicate = isDuplicate
)
