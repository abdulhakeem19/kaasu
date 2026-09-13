package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Maps a raw parsed merchant name to a user-chosen display name, so future captures of the same
// merchant are stored with the preferred name. Keyed on the lowercased/trimmed original for
// case-insensitive matching.
@Entity(tableName = "merchant_aliases")
data class MerchantAliasEntity(
    @PrimaryKey val originalLower: String,
    val displayName: String,
    val createdAt: Long,
    val updatedAt: Long
)
