package com.kaasu.app.notification.filter

/**
 * The strict allowlist of apps Kaasu will even look at. A [NotificationListenerService] sees
 * notifications from every installed app, so we deliberately narrow capture to:
 *
 *  - [PAYMENT_APP_PACKAGES] — UPI / wallet / bank apps whose notifications are real payment events.
 *  - [MESSAGING_APP_PACKAGES] — the system SMS app, which delivers bank transaction SMS.
 *
 * Everything else (food, grocery, shopping, social…) is dropped outright, regardless of content.
 * Users can still add a bank that isn't listed here via Settings → Bank sources (stored in the
 * `app_sources` table), which is unioned with [PAYMENT_APP_PACKAGES] at runtime.
 */
object SourceApps {

    val PAYMENT_APP_PACKAGES = setOf(
        // ── UPI / wallets ────────────────────────────────────────────────
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                         // PhonePe
        "net.one97.paytm",                         // Paytm
        "in.org.npci.upiapp",                      // BHIM
        "com.amazon.mShop.android.shopping",       // Amazon Pay (inside the Amazon app)
        "com.dreamplug.androidapp",                // CRED
        "com.bharatpe.app",                        // BharatPe (consumer)
        "com.bharatpe.merchant",                   // BharatPe (merchant)
        "com.mobikwik_new",                        // MobiKwik
        "com.freecharge.android",                  // Freecharge
        "com.slicepay",                            // Slice
        "com.fampay.in",                           // FamPay
        // ── Bank apps ────────────────────────────────────────────────────
        "com.csam.icici.bank.imobile",             // ICICI iMobile Pay
        "com.sbi.lotusintouch",                    // YONO SBI
        "com.sbi.card",                            // SBI Card
        "com.axis.mobile",                         // Axis Mobile
        "com.mgs.indusind",                        // IndusInd
        "com.idfcfirstbank.optimus",               // IDFC FIRST Bank
        "com.UnionBank.retail",                    // Union Bank
        "com.snapwork.hdfc",                       // HDFC Bank
        "com.msf.kbank.mobile",                    // Kotak
        "com.bankofbaroda.mconnect",               // Bank of Baroda
        "com.canarabank.mobility",                 // Canara Bank
        "com.fss.pnbpsp",                          // PNB
    )

    val MESSAGING_APP_PACKAGES = setOf(
        "com.google.android.apps.messaging", // Google Messages (Pixel, Motorola, OnePlus…)
        "com.samsung.android.messaging",     // Samsung Messages
        "com.android.mms",                   // AOSP / MIUI / many OEMs
        "com.android.messaging",             // AOSP Messaging
        "com.coloros.mms",                   // Oppo / Realme
        "com.vivo.mms",                      // Vivo
        "com.miui.smsextra",                 // Xiaomi
    )

    // Friendly display names for known payment packages (for the "Add bank" picker + details).
    val DISPLAY_NAMES = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.dreamplug.androidapp" to "CRED",
        "com.bharatpe.app" to "BharatPe",
        "com.bharatpe.merchant" to "BharatPe Merchant",
        "com.mobikwik_new" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        "com.slicepay" to "Slice",
        "com.fampay.in" to "FamPay",
        "com.csam.icici.bank.imobile" to "ICICI iMobile Pay",
        "com.sbi.lotusintouch" to "YONO SBI",
        "com.sbi.card" to "SBI Card",
        "com.axis.mobile" to "Axis Mobile",
        "com.mgs.indusind" to "IndusInd",
        "com.idfcfirstbank.optimus" to "IDFC FIRST Bank",
        "com.UnionBank.retail" to "Union Bank",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.msf.kbank.mobile" to "Kotak",
        "com.bankofbaroda.mconnect" to "Bank of Baroda",
        "com.canarabank.mobility" to "Canara Bank",
        "com.fss.pnbpsp" to "PNB",
    )

    fun displayName(pkg: String): String {
        if (pkg.startsWith(SmsFilter.SMS_PACKAGE_PREFIX)) {
            return "${pkg.removePrefix(SmsFilter.SMS_PACKAGE_PREFIX)} (SMS)"
        }
        return DISPLAY_NAMES[pkg] ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}
