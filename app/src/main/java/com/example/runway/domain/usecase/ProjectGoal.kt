package com.example.runway.domain.usecase

import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** BR-76. Copy for each follows the zero-guilt guide in the UI. */
enum class GoalStatus { REACHED, AHEAD, ON_TRACK, BEHIND, NOT_ENOUGH_DATA }

data class GoalProjection(
    val targetMinor: Long,
    val savedMinor: Long,
    val remainingMinor: Long,
    val progress: Float,             // 0..1, drives the runway track
    val percent: Int,                // 0..100
    val monthsLeft: Int,             // at least 1
    val requiredPerMonthMinor: Long, // BR-72
    val paceMinorPerMonth: Long?,    // BR-73: null with under 14 days of history
    val projectedDate: LocalDate?,   // BR-74: null when the pace is unknown or not positive
    val monthsAfterPlan: Int?,       // projected month − deadline month: positive is later than planned
    val startDate: LocalDate,
    val deadline: LocalDate,
    val status: GoalStatus,
)

/**
 * The goal runway (Doc 1 section 8.8). Pure: the caller passes "saved" (the goal fund balance, BR-71)
 * and the transactions, so this is tested without Firebase. Never divides by zero (AT-11).
 */
class ProjectGoal @Inject constructor(
    private val balances: ComputeAccountBalances,
) {

    operator fun invoke(
        goal: Goal,
        savedMinor: Long,
        transactions: List<Transaction>,
        today: LocalDate,
        zone: ZoneId,
    ): GoalProjection {
        val target = goal.targetMinor.coerceAtLeast(1L)
        val saved = savedMinor.coerceAtLeast(0L)
        val remaining = (target - saved).coerceAtLeast(0L)
        val start = goal.startDate.atZone(zone).toLocalDate()
        val deadline = goal.deadline.atZone(zone).toLocalDate()

        val monthsLeft = monthsLeft(today, deadline)
        val required = if (remaining == 0L) 0L else ceilDiv(remaining, monthsLeft.toLong())
        val pace = pace(goal, transactions, start, today, zone)
        val projected = if (pace != null && pace > 0 && remaining > 0) {
            today.plusDays(ceilDiv(remaining * DAYS_PER_MONTH, pace))
        } else {
            null
        }

        return GoalProjection(
            targetMinor = target,
            savedMinor = saved,
            remainingMinor = remaining,
            progress = (saved.toDouble() / target).coerceIn(0.0, 1.0).toFloat(),
            percent = (saved * 100 / target).coerceIn(0L, 100L).toInt(),
            monthsLeft = monthsLeft,
            requiredPerMonthMinor = required,
            paceMinorPerMonth = pace,
            projectedDate = projected,
            monthsAfterPlan = projected?.let {
                ChronoUnit.MONTHS.between(YearMonth.from(deadline), YearMonth.from(it)).toInt()
            },
            startDate = start,
            deadline = deadline,
            status = status(remaining, pace, required),
        )
    }

    /**
     * BR-75: days a purchase pushes the goal back = price ÷ (pace ÷ 30), rounded to the nearest day.
     * Without a positive pace it uses the required rate instead, so it never divides by zero (AT-11).
     */
    fun whatIfDelayDays(priceMinor: Long, projection: GoalProjection): Int {
        if (priceMinor <= 0 || projection.remainingMinor == 0L) return 0 // reached: nothing to push back
        val monthlyRate = projection.paceMinorPerMonth?.takeIf { it > 0 } ?: projection.requiredPerMonthMinor
        if (monthlyRate <= 0) return 0
        val days = BigDecimal.valueOf(priceMinor)
            .multiply(BigDecimal.valueOf(DAYS_PER_MONTH))
            .divide(BigDecimal.valueOf(monthlyRate), 0, RoundingMode.HALF_UP)
            .toInt()
        return days.coerceAtLeast(1)
    }

    /** Whole months to the deadline, a part month counting as one, never below 1 (BR-72). */
    private fun monthsLeft(today: LocalDate, deadline: LocalDate): Int {
        val whole = ChronoUnit.MONTHS.between(today, deadline)
        val months = if (today.plusMonths(whole).isBefore(deadline)) whole + 1 else whole
        return months.coerceAtLeast(1L).toInt()
    }

    /**
     * BR-73: average monthly net change of the goal fund over the last 30 to 90 days. Null until the
     * goal has 14 days of history, so a lucky first week never promises a date.
     */
    private fun pace(goal: Goal, transactions: List<Transaction>, start: LocalDate, today: LocalDate, zone: ZoneId): Long? {
        val historyDays = ChronoUnit.DAYS.between(start, today)
        if (historyDays < MIN_HISTORY_DAYS) return null
        val windowDays = historyDays.coerceAtMost(MAX_WINDOW_DAYS)
        val from = today.minusDays(windowDays).atStartOfDay(zone).toInstant()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant()
        val net = balances.netChange(goal.fundAccountId, transactions, from, to)
        return Math.floorDiv(net * DAYS_PER_MONTH, windowDays)
    }

    /** BR-76: within ±5% of the required rate is on track. */
    private fun status(remaining: Long, pace: Long?, required: Long): GoalStatus = when {
        remaining == 0L -> GoalStatus.REACHED
        pace == null -> GoalStatus.NOT_ENOUGH_DATA
        pace * 100 >= required * 105 -> GoalStatus.AHEAD
        pace * 100 < required * 95 -> GoalStatus.BEHIND
        else -> GoalStatus.ON_TRACK
    }

    /** a ÷ b rounded up, for a >= 0 and b > 0. Math.ceilDiv needs Java 18, which API 26 does not have. */
    private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b

    private companion object {
        const val DAYS_PER_MONTH = 30L
        const val MIN_HISTORY_DAYS = 14L
        const val MAX_WINDOW_DAYS = 90L
    }
}
