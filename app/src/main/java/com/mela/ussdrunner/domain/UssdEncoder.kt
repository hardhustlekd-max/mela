package com.mela.ussdrunner.domain

/**
 * Encodes a USSD string for a tel: URI without turning it into an invalid
 * request. The trailing (or any) '#' must become %23 — otherwise the URI
 * parser treats it as a fragment and the carrier never sees the code.
 */
object UssdEncoder {
    fun encodeForTel(code: String): String {
        val normalized = UssdValidator.normalize(code)
        val encoded = buildString(normalized.length + 4) {
            for (ch in normalized) {
                when (ch) {
                    '#' -> append("%23")
                    '+' -> append("%2B")
                    else -> append(ch)
                }
            }
        }
        return "tel:$encoded"
    }

    fun encodeDigitsOnly(code: String): String = UssdValidator.normalize(code)
}
