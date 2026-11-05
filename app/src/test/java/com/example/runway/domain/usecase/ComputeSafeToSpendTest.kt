package com.example.runway.domain.usecase

import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSourceKind.ADSENSE
import com.example.runway.domain.model.IncomeSourceKind.CRYPTO
import com.example.runway.domain.model.IncomeSourceKind.FREELANCE
import com.example.runway.domain.model.IncomeSourceKind.SALARY
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.RecurringKind
import com.example.runway.domain.model.Reliability.RELIABLE
import com.example.runway.domain.model.Reliability.VARIABLE
import com.example.runway.domain.model.TransactionType.INCOME
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.source
import com.example.runway.testutil.Fixtures.tx
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeSafeToSpendTest {

    private val compute = ComputeSafeToSpend()
    private val tenth = LocalDate.of(2026, 11, 10) // day 10 of a 30-day month

    @Test
    fun `worked example from business rules`() {
        val r = compute(
            SafeToSpendInput(
                reliableIncomeExpected = 12_000_000, variableIncomeReceived = 800_000,
                committedCosts = 4_500_000, goalContribution = 3_990_000,
                discretionarySpent = 1_210_000, today = tenth,
            ),
        )
        assertEquals(4_310_000, r.monthBudget)
        assertEquals(21, r.daysLeft)
        assertEquals(147_619, r.perDay) // LKR 1,476.19
    }

    @Test
    fun `days left includes today, so the last day of the month is 1`() {
        val r = compute(SafeToSpendInput(lkr(1_000), 0, 0, 0, 0, LocalDate.of(2026, 11, 30)))
        assertEquals(1, r.daysLeft)
        assertEquals(lkr(1_000), r.perDay)
    }

    @Test
    fun `spending past the budget goes negative and reads as over plan (Doc 1 section 8_9)`() {
        val r = compute(SafeToSpendInput(lkr(43_100), 0, 0, 0, lkr(46_300), tenth))
        assertEquals(lkr(-3_200), r.remaining)
        assertEquals(SpendPace.OVER, compute.pace(r, lkr(46_300), tenth))
    }

    @Test
    fun `pace compares spending with an even share of the budget so far`() {
        val r = compute(SafeToSpendInput(lkr(30_000), 0, 0, 0, lkr(12_000), tenth))
        assertEquals(SpendPace.FASTER, compute.pace(r, lkr(12_000), tenth)) // fair share by day 10 is 10,000
        val calm = compute(SafeToSpendInput(lkr(30_000), 0, 0, 0, lkr(9_000), tenth))
        assertEquals(SpendPace.ON_PACE, compute.pace(calm, lkr(9_000), tenth))
    }

    @Test
    fun `only reliable sources are expected in advance (BR-43)`() {
        val sources = listOf(
            source("salary", SALARY, RELIABLE, expected = lkr(120_000)),
            source("freelance", FREELANCE, VARIABLE, expected = lkr(50_000)),
        )
        assertEquals(lkr(120_000), compute.reliableIncomeExpected(sources, emptyMap()))
    }

    @Test
    fun `received variable income and the salary bonus count, crypto does not (BR-40, BR-41, BR-62)`() {
        val sources = listOf(
            source("salary", SALARY, RELIABLE, expected = lkr(120_000)),
            source("adsense", ADSENSE, VARIABLE),
            source("crypto", CRYPTO, VARIABLE),
        )
        val month = listOf(
            tx(INCOME, lkr(130_000), at("2026-11-25"), source = "salary"), // 10,000 bonus above base
            tx(INCOME, lkr(8_000), at("2026-11-05"), source = "adsense"),
            tx(INCOME, lkr(20_000), at("2026-11-06"), source = "crypto"),
            tx(INCOME, lkr(1_500), at("2026-11-07"), source = null), // a gift with no source
            tx(INCOME, lkr(9_999), at("2026-11-08"), source = "adsense", archived = true),
        )
        assertEquals(lkr(19_500), compute.variableIncomeReceived(sources, month, emptyMap()))
    }

    @Test
    fun `salary below its base adds no bonus`() {
        val sources = listOf(source("salary", SALARY, RELIABLE, expected = lkr(120_000)))
        val month = listOf(tx(INCOME, lkr(110_000), at("2026-11-25"), source = "salary"))
        assertEquals(0L, compute.variableIncomeReceived(sources, month, emptyMap()))
    }

    @Test
    fun `planned committed costs count monthly items, yearly ones spread, foreign ones converted`() {
        val items = listOf(
            recurring("rent", lkr(34_000), RecurringFrequency.MONTHLY),
            recurring("insurance", lkr(12_000), RecurringFrequency.YEARLY),
            recurring("cloud", 1_000L, RecurringFrequency.MONTHLY, CurrencyCode.USD), // USD 10.00
            recurring("old-gym", lkr(5_000), RecurringFrequency.MONTHLY, active = false),
        )
        assertEquals(lkr(38_000), compute.plannedCommitted(items, mapOf("USD" to 300.0)))
        assertEquals(lkr(35_000), compute.plannedCommitted(items, emptyMap())) // no USD rate yet: left out
    }

    @Test
    fun `committed costs use whichever is larger, plan or actual (Doc 1 section 8_9)`() {
        assertEquals(lkr(45_000), compute.committedCosts(plannedMinor = lkr(45_000), actualCommittedMinor = lkr(34_000)))
        assertEquals(lkr(47_500), compute.committedCosts(plannedMinor = lkr(45_000), actualCommittedMinor = lkr(47_500)))
    }

    private fun recurring(
        id: String,
        amount: Long,
        frequency: RecurringFrequency,
        currency: CurrencyCode = CurrencyCode.LKR,
        active: Boolean = true,
    ) = RecurringItem(
        id = id,
        kind = RecurringKind.BILL,
        name = id,
        amountMinor = amount,
        currency = currency,
        frequency = frequency,
        nextDueDate = at("2026-11-30"),
        categoryId = null,
        accountId = null,
        active = active,
    )
}
