package com.kaasu.app.domain.model

/**
 * Which end of a transfer a row represents.
 *
 * Money moving between two of the owner's accounts produces two rows, one per bank announcement.
 * The role is what lets each row move only its own account: OUT subtracts, IN adds, so a complete
 * group nets to zero and the movement is never counted as spending.
 */
enum class TransferRole {
    OUT,
    IN;

    val opposite: TransferRole get() = if (this == OUT) IN else OUT

    companion object {
        /** Tolerates unknown or absent values from old rows and restored backups. */
        fun fromStorage(value: String?): TransferRole? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}
