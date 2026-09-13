package com.kaasu.app.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.kaasu.app.accessibility.service.KaasuAccessibilityService

/**
 * Read-only reflection of whether the user has flipped [KaasuAccessibilityService] on in Android's
 * system Settings. Kaasu cannot toggle it programmatically (AccessibilityService is never a runtime
 * permission) — only deep-link to [Settings.ACTION_ACCESSIBILITY_SETTINGS] and poll this on resume,
 * the same `DisposableEffect`/`ON_RESUME` pattern already used for notification access and SMS
 * permission in `OnboardingScreen`.
 */
object AccessibilityServiceStatus {

    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, KaasuAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
