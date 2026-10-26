package com.example.runway.core.money

import java.math.BigDecimal
import java.math.RoundingMode

object FxMath {
    /** amountLkrMinor = round(amountMinor x fxRate). Both currencies have 100 minor units. */
    fun toLkrMinor(amountMinor: Long, fxRate: Double): Long =
        BigDecimal(amountMinor).multiply(BigDecimal.valueOf(fxRate))
            .setScale(0, RoundingMode.HALF_UP).longValueExact()

    /** Effective rate = LKR received / foreign amount. Null instead of dividing by zero. */
    fun effectiveRate(lkrMinor: Long, foreignMinor: Long): Double? =
        if (foreignMinor <= 0L) null else lkrMinor.toDouble() / foreignMinor.toDouble()
}