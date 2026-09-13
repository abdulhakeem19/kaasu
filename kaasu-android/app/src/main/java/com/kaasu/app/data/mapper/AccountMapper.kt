package com.kaasu.app.data.mapper

import com.kaasu.app.core.database.entity.AccountEntity
import com.kaasu.app.domain.model.Account
import com.kaasu.app.domain.model.AccountType

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    displayName = displayName,
    lastFourDigits = lastFourDigits,
    accountType = runCatching { AccountType.valueOf(accountType) }.getOrDefault(AccountType.SAVINGS),
    colorArgb = colorArgb,
    isActive = isActive,
    createdAt = createdAt
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    displayName = displayName,
    lastFourDigits = lastFourDigits,
    accountType = accountType.name,
    colorArgb = colorArgb,
    isActive = isActive,
    createdAt = createdAt
)
