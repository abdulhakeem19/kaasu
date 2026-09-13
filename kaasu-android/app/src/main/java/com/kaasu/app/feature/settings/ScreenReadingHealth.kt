package com.kaasu.app.feature.settings

/** Cross-app summary of whether the accessibility screen-reading channel is doing anything. */
data class ScreenReadingHealth(
    val lastSuccessAt: Long? = null,
    val lastAttemptAt: Long? = null,
) {
    val hasEverCaptured: Boolean get() = lastSuccessAt != null
    val hasEverAttempted: Boolean get() = lastAttemptAt != null
}
