package com.example.runway.domain.usecase

import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.TransactionType.TRANSFER
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.colombo
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectGoalTest {

    private val project = ProjectGoal(ComputeAccountBalances())

    private fun macbook(start: String, deadline: String) = Goal(
        id = Goal.MAIN_ID,
        name = "MacBook Pro M4",
        targetMinor = lkr(490_000),
        startDate = at(start),
        deadline = at(deadline),
        fundAccountId = "goal-fund",
        active = true,
    )

    /** Pay Yourself First on the 25th of Aug, Sep and Oct: LKR 39,900 each. */
    private val pyf = listOf("2026-08-25", "2026-09-25", "2026-10-25").map {
        tx(TRANSFER, lkr(39_900), at(it), account = "bank-main", to = "goal-fund")
    }

    @Test
    fun `day zero needs LKR 39,900 a month (AT-09, BR-72)`() {
        val goal = macbook(start = "2026-10-25", deadline = "2027-10-25")
        val p = project(goal, lkr(11_200), emptyList(), LocalDate.of(2026, 10, 25), colombo)

        assertEquals(12, p.monthsLeft)
        assertEquals(lkr(39_900), p.requiredPerMonthMinor)
        assertEquals(2, p.percent)
    }

    @Test
    fun `under 14 days of history shows no pace and no date (AT-10, BR-73)`() {
        val goal = macbook(start = "2026-10-25", deadline = "2027-10-25")
        val p = project(goal, lkr(11_200), pyf, LocalDate.of(2026, 11, 3), colombo)

        assertNull(p.paceMinorPerMonth)
        assertNull(p.projectedDate)
        assertEquals(GoalStatus.NOT_ENOUGH_DATA, p.status)
    }

    @Test
    fun `pace is the fund's average monthly change and gives the landing date (BR-73, BR-74)`() {
        val goal = macbook(start = "2026-08-01", deadline = "2027-08-01")
        val p = project(goal, lkr(91_000), pyf, LocalDate.of(2026, 10, 30), colombo)

        assertEquals(lkr(39_900), p.paceMinorPerMonth) // 119,700 over 90 days, times 30
        assertEquals(lkr(39_900), p.requiredPerMonthMinor) // 399,000 over 10 months
        assertEquals(LocalDate.of(2027, 8, 26), p.projectedDate) // 399,000 ÷ (39,900 ÷ 30) = 300 days
        assertEquals(0, p.monthsAfterPlan)
        assertEquals(GoalStatus.ON_TRACK, p.status)
    }

    @Test
    fun `slower than 95 percent of the required rate is behind, and lands later (BR-76)`() {
        val goal = macbook(start = "2026-08-01", deadline = "2027-04-01")
        val p = project(goal, lkr(91_000), pyf, LocalDate.of(2026, 10, 30), colombo)

        assertEquals(GoalStatus.BEHIND, p.status) // needs 66,500 a month, saving 39,900
        assertEquals(4, p.monthsAfterPlan) // lands Aug 2027 instead of Apr 2027
    }

    @Test
    fun `faster than 105 percent of the required rate is ahead (BR-76)`() {
        val goal = macbook(start = "2026-08-01", deadline = "2028-08-01")
        val p = project(goal, lkr(91_000), pyf, LocalDate.of(2026, 10, 30), colombo)
        assertEquals(GoalStatus.AHEAD, p.status)
    }

    @Test
    fun `a fund that only shrinks has no landing date (BR-74)`() {
        val goal = macbook(start = "2026-08-01", deadline = "2027-08-01")
        val withdrawal = tx(TRANSFER, lkr(20_000), at("2026-10-01"), account = "goal-fund", to = "bank-main")
        val p = project(goal, lkr(91_000), listOf(withdrawal), LocalDate.of(2026, 10, 30), colombo)

        assertEquals(true, p.paceMinorPerMonth!! < 0)
        assertNull(p.projectedDate)
        assertEquals(GoalStatus.BEHIND, p.status)
    }

    @Test
    fun `a part month counts as a whole month left (BR-72)`() {
        val goal = macbook(start = "2026-10-25", deadline = "2027-10-25")
        val p = project(goal, lkr(11_200), emptyList(), LocalDate.of(2026, 11, 3), colombo)
        assertEquals(12, p.monthsLeft) // 11 months and 22 days
    }

    @Test
    fun `a passed deadline asks for the rest now instead of dividing by zero`() {
        val goal = macbook(start = "2025-10-25", deadline = "2026-10-25")
        val p = project(goal, lkr(400_000), emptyList(), LocalDate.of(2026, 11, 3), colombo)

        assertEquals(1, p.monthsLeft)
        assertEquals(lkr(90_000), p.requiredPerMonthMinor)
    }

    @Test
    fun `saved at or above target is reached`() {
        val goal = macbook(start = "2026-08-01", deadline = "2027-08-01")
        val p = project(goal, lkr(495_000), pyf, LocalDate.of(2026, 10, 30), colombo)

        assertEquals(GoalStatus.REACHED, p.status)
        assertEquals(0L, p.requiredPerMonthMinor)
        assertEquals(100, p.percent)
        assertEquals(0, project.whatIfDelayDays(lkr(12_000), p))
    }

    @Test
    fun `what-if uses the pace when there is one (BR-75)`() {
        val p = projectionWith(pace = lkr(30_000), required = lkr(39_900))
        assertEquals(12, project.whatIfDelayDays(lkr(12_000), p)) // Doc 1 section 4.3
    }

    @Test
    fun `what-if falls back to the required rate when the pace is zero (AT-11)`() {
        assertEquals(9, project.whatIfDelayDays(lkr(12_500), projectionWith(pace = 0L, required = lkr(39_900))))
        assertEquals(9, project.whatIfDelayDays(lkr(12_500), projectionWith(pace = null, required = lkr(39_900))))
    }

    @Test
    fun `a tiny purchase still moves the goal by at least a day`() {
        assertEquals(1, project.whatIfDelayDays(lkr(100), projectionWith(pace = lkr(39_900), required = lkr(39_900))))
    }

    private fun projectionWith(pace: Long?, required: Long) = GoalProjection(
        targetMinor = lkr(490_000),
        savedMinor = lkr(186_400),
        remainingMinor = lkr(303_600),
        progress = 0.38f,
        percent = 38,
        monthsLeft = 8,
        requiredPerMonthMinor = required,
        paceMinorPerMonth = pace,
        projectedDate = null,
        monthsAfterPlan = null,
        startDate = LocalDate.of(2026, 6, 1),
        deadline = LocalDate.of(2027, 6, 1),
        status = GoalStatus.ON_TRACK,
    )
}
