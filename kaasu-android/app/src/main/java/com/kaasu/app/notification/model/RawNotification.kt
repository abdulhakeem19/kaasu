package com.kaasu.app.notification.model

data class RawNotification(
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val postedAt: Long = System.currentTimeMillis()
) {
    fun fullText(): String = listOfNotNull(title, text, subText)
        .joinToString(" ")
        .trim()
        .normalizeMathUnicode()
}

// Bank apps (SBI, Union Bank) render notification text with Mathematical Alphanumeric
// Unicode (e.g. U+1D5BA–U+1D5D3 sans-serif lowercase). Normalize to ASCII so keyword
// regexes in the parser match without needing Unicode-aware alternatives.
private fun String.normalizeMathUnicode(): String {
    if (!any { it.isHighSurrogate() }) return this
    val sb = StringBuilder(length)
    codePoints().forEach { cp ->
        val ascii = when {
            cp in 0x1D400..0x1D419 -> 'A'.code + (cp - 0x1D400) // Math Bold A-Z
            cp in 0x1D41A..0x1D433 -> 'a'.code + (cp - 0x1D41A) // Math Bold a-z
            cp in 0x1D5A0..0x1D5B9 -> 'A'.code + (cp - 0x1D5A0) // Math Sans-Serif A-Z
            cp in 0x1D5BA..0x1D5D3 -> 'a'.code + (cp - 0x1D5BA) // Math Sans-Serif a-z
            cp in 0x1D5D4..0x1D5ED -> 'A'.code + (cp - 0x1D5D4) // Math Sans-Serif Bold A-Z
            cp in 0x1D5EE..0x1D607 -> 'a'.code + (cp - 0x1D5EE) // Math Sans-Serif Bold a-z
            else -> cp
        }
        sb.appendCodePoint(ascii)
    }
    return sb.toString()
}
