package com.kaasu.app.domain.repository

interface MerchantAliasRepository {
    // Remembers that transactions parsed as [originalName] should display as [displayName].
    suspend fun setAlias(originalName: String, displayName: String)
}
