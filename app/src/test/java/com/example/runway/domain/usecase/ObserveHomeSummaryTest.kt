package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.CategoryKind.COMMITTED
import com.example.runway.domain.model.CategoryKind.DISCRETIONARY
import com.example.runway.domain.model.GapResolution
import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.IncomeSourceKind.ADSENSE
import com.example.runway.domain.model.IncomeSourceKind.SALARY
import com.example.runway.domain.model.Reliability.RELIABLE
import com.example.runway.domain.model.Reliability.VARIABLE
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType.EXPENSE
import com.example.runway.domain.model.TransactionType.INCOME
import com.example.runway.domain.model.UserProfile
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.category
import com.example.runway.testutil.Fixtures.colombo
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.source
import com.example.runway.testutil.Fixtures.tx
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHomeSummaryTest {

    private val balances = ComputeAccountBalances()
    private val home = ObserveHomeSummary(
        observeLedger = mockk(), // summarise() is pure; the flow wiring is not under test here
        balances = balances,
        safeToSpend = ComputeSafeToSpend(),
        projectGoal = ProjectGoal(balances),
        monthSpending = ComputeMonthSpending(),
        unaccounted = ComputeUnaccounted(),
    )
    private val today = LocalDate.of(2026, 11, 10) // day 10 of a 30-day month

    private val accounts = listOf(
        account("bank-main", checkpoint = lkr(50_000), checkpointDate = at("2026-11-01")),
        account("cash", AccountKind.CASH, checkpointDate = at("2026-11-01")),
        account("goal-fund", AccountKind.GOAL_FUND, checkpoint = lkr(11_200), checkpointDate = at("2026-11-09")),
    )
    private val macbook = Goal(
        id = Goal.MAIN_ID,
        name = "MacBook Pro M4",
        targetMinor = lkr(490_000),
        startDate = at("2026-11-10"),
        deadline = at("2027-11-10"),
        fundAccountId = "goal-fund",
        active = true,
    )
    private val sources = listOf(
        source("salary", SALARY, RELIABLE, expected = lkr(120_000)),
        source("adsense", ADSENSE, VARIABLE),
    )
    private val categories = listOf(
        category("rent", COMMITTED),
        category("utilities", COMMITTED),
        category("gym", COMMITTED),
        category("food-coffee", DISCRETIONARY),
    )
    private val november = listOf(
        tx(INCOME, lkr(8_000), at("2026-11-05"), source = "adsense"),
        tx(EXPENSE, lkr(34_000), at("2026-11-01"), category = "rent"),
        tx(EXPENSE, lkr(6_000), at("2026-11-02"), category = "utilities"),
        tx(EXPENSE, lkr(5_000), at("2026-11-03"), category = "gym"),
        tx(EXPENSE, lkr(12_100), at("2026-11-08"), category = "food-coffee"),
    )

    private fun ledger(
        transactions: List<Transaction> = november,
        checkins: List<BalanceCheckin> = emptyList(),
        profileCreated: String = "2026-11-01",
        goal: Goal? = macbook,
    ) = Ledger(
        now = today.atTime(9, 0).atZone(colombo).toInstant(),
        today = today,
        zone = colombo,
        profile = UserProfile(uid = "u1", displayName = "Kavindu Silva", email = "k@example.com", createdAt = at(profileCreated)),
        accounts = accounts,
        goal = goal,
        sources = sources,
        categories = categories,
        transactions = transactions,
        checkins = checkins,
        recurring = emptyList(),
    )

    @Test
    fun `Doc 1 worked example gives LKR 1,476 a day (Doc 1 section 8_9)`() {
        val s = home.summarise(ledger())

        assertEquals(lkr(39_900), s.projection?.requiredPerMonthMinor) // 478,800 over 12 months
        assertEquals(lkr(43_100), s.safeToSpend.monthBudget)
        assertEquals(21, s.safeToSpend.daysLeft)
        assertEquals(147_619L, s.safeToSpend.perDay)
        assertEquals(SpendPace.ON_PACE, s.pace)
    }

    @Test
    fun `this month splits committed from discretionary (FR-13)`() {
        val s = home.summarise(ledger())
        assertEquals(lkr(45_000), s.month.committedMinor)
        assertEquals(lkr(12_100), s.month.discretionaryMinor)
        assertEquals("rent", s.month.byCategory.first().category?.id)
    }

    @Test
    fun `the greeting uses the first name`() {
        assertEquals("Kavindu", home.summarise(ledger()).firstName)
    }

    @Test
    fun `saved is the goal fund balance (BR-71)`() {
        assertEquals(lkr(11_200), home.summarise(ledger()).projection?.savedMinor)
    }

    @Test
    fun `without a goal nothing is set aside for it`() {
        val s = home.summarise(ledger(goal = null))
        assertNull(s.projection)
        assertEquals(lkr(83_000), s.safeToSpend.monthBudget) // 120,000 + 8,000 − 45,000
    }

    @Test
    fun `three days with no entry or check-in welcomes him back (BR-85, AT-23)`() {
        val old = listOf(tx(EXPENSE, lkr(1_200), at("2026-11-06"), category = "food-coffee"))
        val s = home.summarise(ledger(transactions = old, profileCreated = "2026-10-01"))
        assertEquals(HomePrompt.WelcomeBack(daysAway = 4), s.prompt)
    }

    @Test
    fun `a week without a check-in suggests one`() {
        assertEquals(HomePrompt.CheckinDue(daysSince = null), home.summarise(ledger()).prompt)
    }

    @Test
    fun `a recent check-in and recent entries mean no prompt`() {
        val checkin = BalanceCheckin("c1", at("2026-11-09"), "bank-main", 0L, 0L, 0L, GapResolution.NONE)
        assertNull(home.summarise(ledger(checkins = listOf(checkin))).prompt)
    }

    @Test
    fun `Clarity appears once there is a check-in this month (BR-82)`() {
        assertNull(home.summarise(ledger()).clarityPercent)

        val gap = BalanceCheckin("c1", at("2026-11-09"), "bank-main", lkr(84_000), lkr(90_100), lkr(6_100), GapResolution.UNACCOUNTED)
        // logged 57,100 against 6,100 unaccounted
        assertEquals(90, home.summarise(ledger(checkins = listOf(gap))).clarityPercent)
    }

    @Test
    fun `today's activity is only today, newest first`() {
        val morning = tx(EXPENSE, lkr(850), at("2026-11-10").minusSeconds(3 * 3600), category = "food-coffee", id = "coffee")
        val noon = tx(EXPENSE, lkr(1_200), at("2026-11-10"), category = "food-coffee", id = "kottu")
        val s = home.summarise(ledger(transactions = november + morning + noon))
        assertEquals(listOf("kottu", "coffee"), s.todayActivity.map { it.id })
        assertEquals(lkr(2_050), s.todayActivity.spentMinor())
    }

    @Test
    fun `brand new with nothing seeded yet is empty (AT-01)`() {
        val bare = ledger(transactions = emptyList(), goal = null).copy(accounts = emptyList(), sources = emptyList())
        assertTrue(home.summarise(bare).isEmpty)
        assertFalse(home.summarise(ledger()).isEmpty)
    }
}
