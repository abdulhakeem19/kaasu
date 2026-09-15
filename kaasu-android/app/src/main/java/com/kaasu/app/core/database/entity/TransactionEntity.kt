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
        Index("rawTextHash"),
        Index("transferGroupId")
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
    val isDuplicate: Boolean = false,
    // The two legs of one movement between the owner's own accounts share this id. Null for
    // ordinary payments. Kept as a group rather than a link because each bank announces its own
    // leg separately and each leg needs to stay independently de-duplicable.
    val transferGroupId: String? = null,
    // "OUT" or "IN" — which end of the movement this row is. A group may legitimately have only
    // one leg: a credit card usually says nothing when its bill is paid.
    val transferRole: String? = null,
    // The account at the other end. Display and repair only — never summed, since each row already
    // moves its own account and summing both ends would double the movement.
    val counterpartAccountId: Long? = null
)
