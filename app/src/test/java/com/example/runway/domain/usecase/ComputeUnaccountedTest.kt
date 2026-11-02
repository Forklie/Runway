package com.example.runway.domain.usecase

import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.GapResolution
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.lkr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeUnaccountedTest {

    private val compute = ComputeUnaccounted()

    @Test
    fun `worked example: expected 90,100, entered 84,000 leaves a 6,100 gap (BR-81, AT-12)`() {
        assertEquals(lkr(6_100), compute.gap(expectedMinor = lkr(90_100), enteredMinor = lkr(84_000)))
    }

    @Test
    fun `more money than expected is a negative gap (BR-81)`() {
        assertEquals(lkr(-1_000), compute.gap(expectedMinor = lkr(5_000), enteredMinor = lkr(6_000)))
    }

    @Test
    fun `worked example: Clarity is about 91 percent (BR-82)`() {
        assertEquals(91, compute.clarityPercent(loggedExpensesMinor = lkr(62_000), unaccountedMinor = lkr(6_100)))
    }

    @Test
    fun `nothing logged and nothing unaccounted has no Clarity yet`() {
        assertNull(compute.clarityPercent(0L, 0L))
    }

    @Test
    fun `only positive gaps left as Unaccounted count against Clarity (BR-84)`() {
        val checkins = listOf(
            checkin("bank-main", gap = lkr(6_100), GapResolution.UNACCOUNTED),
            checkin("cash", gap = lkr(1_200), GapResolution.LOGGED), // now a logged expense
            checkin("secondary", gap = lkr(-800), GapResolution.UNACCOUNTED), // extra money, not spending
            checkin("goal-fund", gap = 0L, GapResolution.NONE),
        )
        assertEquals(lkr(6_100), compute.unaccountedSpending(checkins))
    }

    @Test
    fun `a zero gap needs no resolution, any other defaults to Unaccounted`() {
        assertEquals(GapResolution.NONE, compute.defaultResolution(0L))
        assertEquals(GapResolution.UNACCOUNTED, compute.defaultResolution(lkr(-50)))
    }

    private fun checkin(account: String, gap: Long, resolution: GapResolution) = BalanceCheckin(
        id = "checkin-$account",
        date = at("2026-11-01"),
        accountId = account,
        enteredBalanceMinor = 0L,
        expectedBalanceMinor = gap,
        gapMinor = gap,
        resolution = resolution,
    )
}
