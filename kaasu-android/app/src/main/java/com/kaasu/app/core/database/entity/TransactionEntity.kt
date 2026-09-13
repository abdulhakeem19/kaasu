package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("transactionTime"),
        Index("categoryId"),
        Index("sourceAppPackage"),
        Index("rawTextHash")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountInPaise: Long,
    val currency: String = "INR",
    val type: String,
    val merchantName: String?,
    val categoryId: Long?,
    val sourceAppPackage: String?,
    val sourceAppName: String?,
    val paymentMode: String?,
    val rawText: String?,
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
    // When set, this is one slice of a split; the parent (isIgnored=true) holds the original payment.
    val parentId: Long? = null,
    // User marked this as a duplicate; also set isIgnored=true so it drops out of lists and totals.
    val isDuplicate: Boolean = false
)
