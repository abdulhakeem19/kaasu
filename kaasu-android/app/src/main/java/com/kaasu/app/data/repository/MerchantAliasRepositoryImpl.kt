package com.kaasu.app.data.repository

import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.database.entity.MerchantAliasEntity
import com.kaasu.app.domain.repository.MerchantAliasRepository
import javax.inject.Inject

class MerchantAliasRepositoryImpl @Inject constructor(
    private val dao: MerchantAliasDao
) : MerchantAliasRepository {

    override suspend fun setAlias(originalName: String, displayName: String) {
        val now = System.currentTimeMillis()
        dao.upsert(
            MerchantAliasEntity(
                originalLower = originalName.trim().lowercase(),
                displayName = displayName.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
