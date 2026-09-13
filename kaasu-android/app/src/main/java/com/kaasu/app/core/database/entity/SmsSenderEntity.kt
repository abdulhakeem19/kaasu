package com.kaasu.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Mirrors AppSourceEntity field-for-field, keyed on SMS sender id instead of package name.
@Entity(
    tableName = "sms_senders",
    indices = [Index("senderId", unique = true)]
)
data class SmsSenderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val displayLabel: String?,
    val isEnabled: Boolean = true,
    val isKnownFinanceSender: Boolean = false,
    val lastSeenAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
