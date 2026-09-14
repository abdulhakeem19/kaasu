package com.kaasu.app.notification.filter

/**
 * Whether Kaasu can read SMS directly. Kept as a one-method interface so [NotificationFilter] has
 * no Android dependency and can be unit tested; the real implementation checks the runtime
 * permission.
 */
fun interface SmsCaptureAvailability {
    fun isGranted(): Boolean
}
