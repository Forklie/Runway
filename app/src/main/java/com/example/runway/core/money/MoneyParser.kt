package com.example.runway.core.money

import java.math.RoundingMode

object MoneyParser {
    private const val MAX_MINOR = 10_000_000_000L // LKR 100,000,000

    /** "1,250.50" -> 125050. Null if invalid, <= 0, more than 2 decimals, or too large. */
    fun parseToMinor(input: String): Long? {
        val value = input.replace(",", "").trim().toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2) return null
        val minor = runCatching {
            value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        }.getOrNull() ?: return null
        return minor.takeIf { it in 1..MAX_MINOR }
    }

    /** Filters keystrokes: digits and one dot, max 9 whole digits, 2 decimals. */
    fun sanitizeInput(raw: String): String {
        val cleaned = raw.filter { it in '0'..'9' || it == '.' }
        val dot = cleaned.indexOf('.')
        if (dot == -1) return cleaned.take(9)
        val whole = cleaned.substring(0, dot).take(9)
        val fraction = cleaned.substring(dot + 1).filter { it in '0'..'9' }.take(2)
        return "$whole.$fraction"
    }
}