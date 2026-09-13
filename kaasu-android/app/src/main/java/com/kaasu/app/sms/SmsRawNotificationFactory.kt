package com.kaasu.app.sms

import com.kaasu.app.notification.filter.SmsFilter
import com.kaasu.app.notification.model.RawNotification

// Shared between SmsReceiver (live capture) and SmsBackfillWorker (historical inbox scan) so both
// build the exact same RawNotification shape for a given (sender, body, timestamp) triple.
object SmsRawNotificationFactory {

    // Strip whitespace, uppercase, keep as the raw sender otherwise — don't overengineer normalization.
    fun normalizeSenderId(address: String): String = address.trim().uppercase()

    // Returns null when there's no sender to classify against (can't run SmsFilter without one).
    fun build(address: String?, body: String, timestampMillis: Long): RawNotification? {
        val sender = address?.takeIf { it.isNotBlank() } ?: return null
        return RawNotification(
            packageName = "${SmsFilter.SMS_PACKAGE_PREFIX}${normalizeSenderId(sender)}",
            appName = null,
            title = null,
            text = body,
            subText = null,
            postedAt = timestampMillis
        )
    }
}
