package com.kaasu.app.notification.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RawNotificationTest {

    private fun notification(title: String? = null, text: String? = null, subText: String? = null) =
        RawNotification(packageName = "test", appName = null, title = title, text = text, subText = subText)

    // ── fullText joining ──────────────────────────────────────────────────────

    @Test fun fullText_joinsTitleAndText() {
        val n = notification(title = "Payment", text = "₹500 paid")
        assertEquals("Payment ₹500 paid", n.fullText())
    }

    @Test fun fullText_skipsNullFields() {
        val n = notification(text = "₹500 paid")
        assertEquals("₹500 paid", n.fullText())
    }

    @Test fun fullText_allNull_returnsEmpty() {
        assertEquals("", notification().fullText())
    }

    // ── Mathematical Sans-Serif Unicode normalization ─────────────────────────

    // Union Bank notification: "𝖸𝗈𝗎𝗋 𝖲𝖡 A/c *0000 𝖢𝗋𝖾𝖽𝗂𝗍𝖾𝖽 𝖿𝗈𝗋 Rs:1.00"
    @Test fun fullText_normalizesMathSansSerifLowercase() {
        val mathSpent = "𝗌𝗉𝖾𝗇𝗍" // 𝗌𝗉𝖾𝗇𝗍
        val n = notification(text = "Rs.381.23 $mathSpent on your card")
        assertFalse(n.fullText().any { it.isHighSurrogate() })
        assertEquals("Rs.381.23 spent on your card", n.fullText())
    }

    @Test fun fullText_normalizesMathSansSerifUppercase() {
        val mathYour = "𝖸𝗈𝗎𝗋" // 𝖸𝗈𝗎𝗋
        val n = notification(text = "$mathYour account")
        assertEquals("Your account", n.fullText())
    }

    @Test fun fullText_normalizesMathBold() {
        val mathBoldHello = "𝐇𝐞𝐥𝐥𝐨" // 𝐇𝐞𝐥𝐥𝐨
        val n = notification(text = "$mathBoldHello world")
        assertEquals("Hello world", n.fullText())
    }

    @Test fun fullText_noSurrogates_returnsFastPath() {
        val plain = "Your A/c debited Rs.500"
        val n = notification(text = plain)
        assertEquals(plain, n.fullText())
    }

    @Test fun fullText_mixedAsciiAndMathUnicode_normalizedCorrectly() {
        // "Rs.381.23 𝗌𝗉𝖾𝗇𝗍 on your SBI Credit Card"
        val mathSpent = "𝗌𝗉𝖾𝗇𝗍"
        val n = notification(text = "Rs.381.23 $mathSpent on your SBI Credit Card")
        assertEquals("Rs.381.23 spent on your SBI Credit Card", n.fullText())
    }
}
