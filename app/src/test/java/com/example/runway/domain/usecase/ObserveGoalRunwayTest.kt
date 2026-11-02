package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType.TRANSFER
import com.example.runway.domain.model.UserProfile
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.colombo
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveGoalRunwayTest {

    private val balances = ComputeAccountBalances()
    private val runway = ObserveGoalRunway(mockk(), balances, ProjectGoal(balances))
    private val today = LocalDate.of(2026, 11, 10)

    private val accounts = listOf(
        account("bank-main", checkpoint = lkr(90_000), checkpointDate = at("2026-11-01")),
        account("cash", AccountKind.CASH, checkpoint = lkr(5_700), checkpointDate = at("2026-11-01")),
        account("goal-fund", AccountKind.GOAL_FUND, checkpoint = lkr(11_200), checkpointDate = at("2026-11-01")),
    )
    private val goal = Goal(Goal.MAIN_ID, "MacBook Pro M4", lkr(490_000), at("2026-11-01"), at("2027-11-01"), "goal-fund", true)

    private fun ledger(goal: Goal?, transactions: List<Transaction> = emptyList()) = Ledger(
        now = at("2026-11-10"),
        today = today,
        zone = colombo,
        profile = UserProfile(uid = "u1", displayName = "Kavindu", email = "k@example.com", createdAt = at("2026-11-01")),
        accounts = accounts,
        goal = goal,
        sources = emptyList(),
        categories = emptyList(),
        transactions = transactions,
        checkins = emptyList(),
        recurring = emptyList(),
    )

    @Test
    fun `no goal yet means nothing to draw`() {
        assertNull(runway.summarise(ledger(goal = null)))
    }

    @Test
    fun `saved follows the fund, including transfers into it (BR-71, BR-33)`() {
        val pyf = tx(TRANSFER, lkr(39_900), at("2026-11-05"), account = "bank-main", to = "goal-fund")
        val r = runway.summarise(ledger(goal, listOf(pyf)))!!
        assertEquals(lkr(51_100), r.projection.savedMinor)
    }

    @Test
    fun `money can be added from every account except the fund itself`() {
        val r = runway.summarise(ledger(goal))!!
        assertEquals(listOf("bank-main", "cash"), r.fromAccounts.map { it.account.id })
        assertEquals(lkr(90_000), r.fromAccounts.first().balanceMinor)
    }
}
