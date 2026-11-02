package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import com.example.runway.domain.model.GapResolution
import com.example.runway.domain.model.IncomeSourceKind.SALARY
import com.example.runway.domain.model.Reliability.RELIABLE
import com.example.runway.domain.model.TransactionType.EXPENSE
import com.example.runway.domain.model.TransactionType.INCOME
import com.example.runway.domain.model.UserProfile
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.colombo
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.source
import com.example.runway.testutil.Fixtures.tx
import io.mockk.mockk
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveInsightsTest {

    private val insights = ObserveInsights(
        observeLedger = mockk(),
        cryptoRepository = mockk(),
        balances = ComputeAccountBalances(),
        incomeReality = ComputeIncomeReality(),
        monthSpending = ComputeMonthSpending(),
        unaccounted = ComputeUnaccounted(),
        cryptoPosition = ComputeCryptoPosition(),
    )
    private val today = LocalDate.of(2026, 11, 10)

    private fun ledger(checkins: List<BalanceCheckin> = emptyList()) = Ledger(
        now = at("2026-11-10"),
        today = today,
        zone = colombo,
        profile = UserProfile(uid = "u1", displayName = "Kavindu", email = "k@example.com", createdAt = at("2026-08-01")),
        accounts = listOf(
            account("bank-main", checkpoint = lkr(50_000), checkpointDate = at("2026-10-01")),
            account("cash", AccountKind.CASH, checkpoint = lkr(2_000), checkpointDate = at("2026-10-01")),
        ),
        goal = null,
        sources = listOf(source("salary", SALARY, RELIABLE, expected = lkr(120_000))),
        categories = emptyList(),
        transactions = listOf(
            tx(INCOME, lkr(130_000), at("2026-10-25"), source = "salary"),
            tx(EXPENSE, lkr(62_000), at("2026-11-04")),
        ),
        checkins = checkins,
        recurring = emptyList(),
    )

    @Test
    fun `balances are derived from checkpoints and movements (BR-31)`() {
        val r = insights.summarise(ledger(), emptyList())
        assertEquals(lkr(118_000), r.balances.first { it.account.id == "bank-main" }.balanceMinor)
        assertEquals(lkr(2_000), r.balances.first { it.account.id == "cash" }.balanceMinor)
    }

    @Test
    fun `income reality uses complete months only (BR-44)`() {
        val r = insights.summarise(ledger(), emptyList())
        assertEquals(1, r.incomeReality.monthsCounted)
        assertEquals(lkr(130_000), r.incomeReality.totalAverageMinor)
    }

    @Test
    fun `no check-in yet means no date and no Clarity`() {
        val r = insights.summarise(ledger(), emptyList())
        assertNull(r.lastCheckin)
        assertNull(r.clarityPercent)
    }

    @Test
    fun `the last check-in and this month's Clarity are reported (BR-82)`() {
        val gap = BalanceCheckin("c1", at("2026-11-07"), "bank-main", lkr(84_000), lkr(90_100), lkr(6_100), GapResolution.UNACCOUNTED)
        val r = insights.summarise(ledger(listOf(gap)), emptyList())
        assertEquals(3, r.daysSinceCheckin)
        assertEquals(91, r.clarityPercent) // 62,000 ÷ 68,100
    }

    @Test
    fun `crypto stays a separate net position (BR-61, BR-62)`() {
        val entries = listOf(
            CryptoEntry("d1", CryptoEntryType.DEPOSIT, "USDT", lkr(100_000), at("2026-06-01"), ""),
            CryptoEntry("s1", CryptoEntryType.HOLDING_SNAPSHOT, "USDT", lkr(112_000), at("2026-10-01"), ""),
        )
        val r = insights.summarise(ledger(), entries)
        assertEquals(lkr(12_000), r.crypto.netMinor)
        assertFalse(insights.summarise(ledger(), emptyList()).hasCryptoEntries)
    }
}
