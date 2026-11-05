package com.example.runway.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Values from Doc 1 (BR-72, AT-09), in minor units: LKR 1 = 100. */
class ComputeDayZeroPreviewTest {

    private val compute = ComputeDayZeroPreview()

    private fun input(
        target: Long = 49_000_000, // LKR 490,000
        saved: Long = 1_120_000,   // LKR 11,200
        months: Int = 12,
        salary: Long = 12_000_000, // LKR 120,000
        rent: Long = 3_400_000,    // LKR 34,000
        estimate: Long = 0,
    ) = DayZeroInput(target, saved, months, salary, rent, estimate)

    @Test
    fun `worked example from the business rules is LKR 39,900 a month (AT-09)`() {
        // (490,000 - 11,200) / 12 = 39,900
        assertEquals(3_990_000L, compute(input()).requiredPerMonthMinor)
    }

    @Test
    fun `share of salary is rounded to a whole percent`() {
        assertEquals(33, compute(input(salary = 12_000_000)).sharePercent) // 33.25
        assertEquals(30, compute(input(salary = 13_300_000)).sharePercent) // exactly 30
    }

    @Test
    fun `left after rent and after the goal`() {
        val result = compute(input())
        assertEquals(8_600_000L, result.leftAfterRentMinor)        // 120,000 - 34,000
        assertEquals(4_610_000L, result.leftAfterRentAndGoalMinor) // 86,000 - 39,900
        assertEquals(Feasibility.FITS, result.feasibility)
    }

    @Test
    fun `own estimate versus reliable income`() {
        // He thinks 210,000, the reliable base is 120,000
        assertEquals(9_000_000L, compute(input(estimate = 21_000_000)).estimateGapMinor)
    }

    @Test
    fun `no estimate means no gap`() {
        assertNull(compute(input(estimate = 0)).estimateGapMinor)
    }

    @Test
    fun `zero salary never divides by zero and says unknown`() {
        val result = compute(input(salary = 0))
        assertEquals(3_990_000L, result.requiredPerMonthMinor)
        assertNull(result.sharePercent)
        assertNull(result.leftAfterRentMinor)
        assertNull(result.leftAfterRentAndGoalMinor)
        assertNull(result.estimateGapMinor)
        assertEquals(Feasibility.UNKNOWN, result.feasibility)
    }

    @Test
    fun `saved at or above the target needs nothing more`() {
        val atTarget = compute(input(saved = 49_000_000))
        assertEquals(0L, atTarget.requiredPerMonthMinor)
        assertEquals(0, atTarget.sharePercent)

        val above = compute(input(saved = 60_000_000))
        assertEquals(0L, above.requiredPerMonthMinor)
        assertEquals(Feasibility.FITS, above.feasibility)
    }

    @Test
    fun `zero or negative months count as one month`() {
        assertEquals(47_880_000L, compute(input(months = 0)).requiredPerMonthMinor)
        assertEquals(47_880_000L, compute(input(months = -3)).requiredPerMonthMinor)
    }

    @Test
    fun `an uneven split rounds up so the plan never falls short`() {
        // 100 minor units over 3 months: 33.33 rounds up to 34
        assertEquals(34L, compute(input(target = 100, saved = 0, months = 3)).requiredPerMonthMinor)
    }

    @Test
    fun `a goal bigger than salary minus rent is a stretch, not an error`() {
        val result = compute(input(salary = 6_000_000, rent = 3_400_000))
        assertEquals(Feasibility.STRETCH, result.feasibility)
        assertEquals(true, result.leftAfterRentAndGoalMinor!! < 0)
    }

    @Test
    fun `rent not entered still gives the left after the goal`() {
        val result = compute(input(rent = 0))
        assertNull(result.leftAfterRentMinor)
        assertEquals(8_010_000L, result.leftAfterRentAndGoalMinor) // 120,000 - 39,900
    }
}
