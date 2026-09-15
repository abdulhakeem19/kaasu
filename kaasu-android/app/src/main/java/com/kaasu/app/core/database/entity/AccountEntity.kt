package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index("lastFourDigits")]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val lastFourDigits: String?,
    val accountType: String,
    val colorArgb: Int?,
    val isActive: Boolean = true,
    val createdAt: Long,
    // The anchor the balance counts forward from. Null means unset — deliberately distinct from
    // zero, which would be a balance the app is claiming to know and does not.
    val openingBalanceInPaise: Long? = null,
    val openingBalanceAt: Long? = null,
    // What the bank itself last stated ("Avl Bal: Rs.11,980"). Advisory: shown beside the derived
    // figure so a disagreement is visible, never silently used as the balance.
    val lastStatedBalanceInPaise: Long? = null,
    val lastStatedBalanceAt: Long? = null,
    // Credit cards only.
    val creditLimitInPaise: Long? = null,
    val statementDay: Int? = null,
    val dueDay: Int? = null
)
