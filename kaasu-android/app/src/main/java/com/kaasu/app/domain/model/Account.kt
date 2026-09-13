package com.kaasu.app.domain.model

data class Account(
    val id: Long = 0,
    val displayName: String,
    val lastFourDigits: String?,
    val accountType: AccountType,
    val colorArgb: Int?,
    val isActive: Boolean = true,
    val createdAt: Long
)
