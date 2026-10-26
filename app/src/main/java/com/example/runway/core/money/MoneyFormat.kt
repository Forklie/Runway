package com.example.runway.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

private const val MINUS = "\u2212"      // true minus sign
private const val THIN_SPACE = "\u2009"

private fun numberFormat(cents: Boolean): NumberFormat =
    NumberFormat.getNumberInstance(Locale.US).apply { // commas, never lakhs
        minimumFractionDigits = if (cents) 2 else 0
        maximumFractionDigits = if (cents) 2 else 0
        roundingMode = RoundingMode.HALF_EVEN
    }

/** 3_990_000 -> "39,900". Summaries drop cents; detail screens pass cents = true. */
fun Long.formatAmount(cents: Boolean = false): String {
    val value = BigDecimal.valueOf(this, 2)
    val text = numberFormat(cents).format(value.abs())
    val isZero = text.none { it in '1'..'9' }
    return if (value.signum() < 0 && !isZero) MINUS + text else text
}

/** "LKR 39,900". Use cents = true on detail screens: "LKR 1,476.19". */
fun Long.formatLkr(cents: Boolean = false): String = "LKR " + formatAmount(cents)

/** Expense row: "− 1,200" (true minus, thin space). */
fun expenseText(minor: Long, cents: Boolean = false): String =
    MINUS + THIN_SPACE + minor.absoluteValue.formatAmount(cents)

/** Income row: "+ 36,240". */
fun incomeText(minor: Long, cents: Boolean = false): String =
    "+" + THIN_SPACE + minor.absoluteValue.formatAmount(cents)

/** Spoken form for TalkBack: "minus 1,200 rupees". */
fun spokenAmount(minor: Long, expense: Boolean, cents: Boolean = false): String =
    (if (expense) "minus " else "") + minor.absoluteValue.formatAmount(cents) + " rupees"