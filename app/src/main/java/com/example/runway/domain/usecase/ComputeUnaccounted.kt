package com.example.runway.domain.usecase

import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.GapResolution
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Reconciliation (Doc 1 section 8.10): the weekly check-in keeps totals true even after missed days.
 * A gap is "Unaccounted", shown in neutral colours, never as a failure.
 */
class ComputeUnaccounted @Inject constructor() {

    /** BR-81: expected − entered. Positive is unlogged spending, negative unlogged income or a refund. */
    fun gap(expectedMinor: Long, enteredMinor: Long): Long = expectedMinor - enteredMinor

    /** How a gap is stored when the user does not log it. A zero gap needs no resolution. */
    fun defaultResolution(gapMinor: Long): GapResolution =
        if (gapMinor == 0L) GapResolution.NONE else GapResolution.UNACCOUNTED

    /**
     * Positive gaps the user left as Unaccounted. Gaps logged as an expense are already in the
     * logged total, so counting them here too would count them twice.
     */
    fun unaccountedSpending(checkins: List<BalanceCheckin>): Long =
        checkins.filter { it.resolution == GapResolution.UNACCOUNTED && it.gapMinor > 0 }.sumOf { it.gapMinor }

    /**
     * BR-82: Clarity = logged ÷ (logged + unaccounted), as a whole percent. Null when there is
     * nothing to judge yet, so a brand-new month never shows a scary 0%.
     */
    fun clarityPercent(loggedExpensesMinor: Long, unaccountedMinor: Long): Int? {
        val logged = loggedExpensesMinor.coerceAtLeast(0L)
        val total = logged + unaccountedMinor.coerceAtLeast(0L)
        if (total == 0L) return null
        return (logged * 100.0 / total).roundToInt()
    }
}
