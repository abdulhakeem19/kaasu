package com.kaasu.app.domain.model

data class Account(
    val id: Long = 0,
    val displayName: String,
    val lastFourDigits: String?,
    val accountType: AccountType,
    val colorArgb: Int?,
    val isActive: Boolean = true,
    val createdAt: Long,
    // Null means unset, which is not the same as zero — see AccountEntity.
    val openingBalanceInPaise: Long? = null,
    val openingBalanceAt: Long? = null,
    val lastStatedBalanceInPaise: Long? = null,
    val lastStatedBalanceAt: Long? = null,
    val creditLimitInPaise: Long? = null,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
) {
    /** Until an opening balance is set, any figure would be a guess dressed up as fact. */
    val hasOpeningBalance: Boolean get() = openingBalanceInPaise != null
}
