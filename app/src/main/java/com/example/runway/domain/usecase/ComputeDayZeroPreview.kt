package com.example.runway.domain.usecase

import javax.inject.Inject

/** What the day-zero preview needs. All money is Long minor units; 0 means "not entered". */
data class DayZeroInput(
    val targetMinor: Long,
    val savedMinor: Long,
    val months: Int,
    val salaryMinor: Long,
    val rentMinor: Long,
    val ownEstimateMinor: Long,
)

enum class Feasibility {
    /** No salary entered, so we cannot say. */
    UNKNOWN,

    /** Rent plus the goal fit inside the reliable salary. */
    FITS,

    /** Rent plus the goal are more than the reliable salary. Shown calmly, never as an alarm. */
    STRETCH,
}

data class DayZeroPreview(
    val requiredPerMonthMinor: Long,
    /** Whole percent of the salary the goal takes. Null when no salary was entered. */
    val sharePercent: Int?,
    /** Salary minus rent. Null when salary or rent is missing. */
    val leftAfterRentMinor: Long?,
    /** Salary minus rent minus the goal: what everything else must fit into. Null without a salary. */
    val leftAfterRentAndGoalMinor: Long?,
    /** Own estimate minus reliable salary. Null when either is missing. */
    val estimateGapMinor: Long?,
    val feasibility: Feasibility,
)

/**
 * FR-06 / BR-72. Pure Kotlin: no Android, no Firebase, never throws.
 * Required per month = (target - saved) / months, rounded UP so the plan never falls short.
 */
class ComputeDayZeroPreview @Inject constructor() {

    operator fun invoke(input: DayZeroInput): DayZeroPreview {
        val months = input.months.coerceAtLeast(1)
        val remaining = (input.targetMinor - input.savedMinor).coerceAtLeast(0L)
        val required = ceilDiv(remaining, months.toLong())

        val salary = input.salaryMinor
        val hasSalary = salary > 0L

        val share = if (hasSalary) ((required * 100 + salary / 2) / salary).toInt() else null
        val leftAfterRent = if (hasSalary && input.rentMinor > 0L) salary - input.rentMinor else null
        val leftAfterGoal = if (hasSalary) salary - input.rentMinor - required else null
        val estimateGap =
            if (hasSalary && input.ownEstimateMinor > 0L) input.ownEstimateMinor - salary else null

        val feasibility = when {
            leftAfterGoal == null -> Feasibility.UNKNOWN
            leftAfterGoal >= 0L -> Feasibility.FITS
            else -> Feasibility.STRETCH
        }

        return DayZeroPreview(
            requiredPerMonthMinor = required,
            sharePercent = share,
            leftAfterRentMinor = leftAfterRent,
            leftAfterRentAndGoalMinor = leftAfterGoal,
            estimateGapMinor = estimateGap,
            feasibility = feasibility,
        )
    }

    private fun ceilDiv(value: Long, divisor: Long): Long = (value + divisor - 1) / divisor
}
