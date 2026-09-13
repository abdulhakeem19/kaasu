package com.kaasu.app.notification.parser

import com.kaasu.app.domain.model.TransactionType
import com.kaasu.app.notification.filter.PendingPaymentDetector
import com.kaasu.app.notification.filter.PromotionalDetector
import com.kaasu.app.notification.filter.SmsFilter
import com.kaasu.app.notification.model.ParsedTransaction
import com.kaasu.app.notification.model.RawNotification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionParser @Inject constructor() {

    // Called by KaasuNotificationListenerService.process(); null return skips DuplicateChecker and CategoryRuleEngine entirely
    fun parse(notification: RawNotification): ParsedTransaction? {
        val text = notification.fullText()
        if (text.isBlank()) return null

        // Offers/deals that merely quote a rupee amount are not transactions — drop them up front
        if (PromotionalDetector.isPromotional(text)) return null

        // Mandate pre-debit notices, scheduled autopay and bill reminders announce money that has
        // not moved yet. The real confirmation arrives days later, far outside DuplicateChecker's
        // window, so storing both double-counts the spend.
        if (PendingPaymentDetector.isPending(text)) return null

        val amountInPaise = AmountParser.parse(text) ?: return null

        val type = TransactionTypeParser.parse(text)
        // No debit/credit direction = we cannot tell if money moved in or out, so it is not a
        // usable transaction. This also removes amount-only promos that slipped past the detector.
        if (type == TransactionType.UNKNOWN) return null

        val merchant = MerchantParser.parse(text, type)
        val confidence = confidence(type, merchant, text, notification.packageName)

        if (confidence < MIN_CONFIDENCE) return null

        return ParsedTransaction(
            amountInPaise = amountInPaise,
            type = type,
            merchantName = merchant,
            sourceAppPackage = notification.packageName,
            sourceAppName = notification.appName,
            rawText = text,
            confidenceScore = confidence,
            transactionTime = notification.postedAt,
            isRecurring = RecurringDetector.isRecurring(text),
            note = NoteParser.parse(text)
        )
    }

    // Evidence-based score. Amount and a resolved direction are both guaranteed by the time we get
    // here (UNKNOWN is rejected above), so the floor is 70. Merchant and strong receipt evidence
    // (account/card tail, UPI/txn reference) push it higher. Known-app membership is a minor nudge,
    // not a decisive boost — a promo from a finance app should no longer ride in on package alone.
    private fun confidence(type: TransactionType, merchant: String?, text: String, packageName: String): Int {
        var score = 40 // amount found (guaranteed at this point)
        if (type != TransactionType.UNKNOWN) score += 30 // resolved direction (guaranteed here)
        if (merchant != null) score += 15
        if (hasStrongTransactionalEvidence(text)) score += 15
        if (packageName in KNOWN_FINANCE_PACKAGES) score += 5
        if (hasDltBankSenderHeader(packageName)) score += 5
        return score
    }

    // Additive-only nudge: a raw SMS whose sender is a standard 2-letter-DLT-prefix bank header
    // (e.g. "sms:HD-HDFCBK") gets the same small confidence bump KNOWN_FINANCE_PACKAGES gives —
    // never a hard gate, since DLT header formats vary too much by telecom circle to rely on.
    private fun hasDltBankSenderHeader(packageName: String): Boolean {
        if (!packageName.startsWith(SmsFilter.SMS_PACKAGE_PREFIX)) return false
        val senderId = packageName.removePrefix(SmsFilter.SMS_PACKAGE_PREFIX)
        return DLT_SENDER_PATTERN.matches(senderId)
    }

    // Account/card tails and reference numbers are present on real bank/UPI receipts and almost
    // never on promotional copy.
    private fun hasStrongTransactionalEvidence(text: String): Boolean {
        val lower = text.lowercase()
        return EVIDENCE_MARKERS.any { lower.contains(it) } || CARD_ENDING_PATTERN.containsMatchIn(text)
    }

    companion object {
        // Amount + a resolved direction (the mandatory minimum) scores exactly 70.
        const val MIN_CONFIDENCE = 70

        private val EVIDENCE_MARKERS = listOf("a/c", "acct", "upi ref", "ref no", "refno", "txn id", "transaction id")
        private val CARD_ENDING_PATTERN = Regex("""(?:x{2,}|ending\s+)\d{3,4}""", RegexOption.IGNORE_CASE)
        private val DLT_SENDER_PATTERN = Regex("""^[A-Z]{2}-[A-Z0-9]{6}$""", RegexOption.IGNORE_CASE)

        val KNOWN_FINANCE_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.amazon.mShop.android.shopping",
            "com.csam.icici.bank.imobile",
            "com.sbi.lotusintouch",
            "com.axis.mobile",
            "com.mgs.indusind",
            "com.idfcfirstbank.optimus",
            "com.sbi.card",
            "com.slicepay",
            "com.UnionBank.retail",
            "com.dreamplug.androidapp",
        )
    }
}
