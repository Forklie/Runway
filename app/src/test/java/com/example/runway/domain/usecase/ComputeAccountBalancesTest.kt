package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.TransactionType.EXPENSE
import com.example.runway.domain.model.TransactionType.INCOME
import com.example.runway.domain.model.TransactionType.REFUND
import com.example.runway.domain.model.TransactionType.TRANSFER
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeAccountBalancesTest {

    private val balances = ComputeAccountBalances()
    private val bank = account("bank-main", checkpoint = lkr(50_000), checkpointDate = at("2026-10-01"))

    @Test
    fun `worked example from the business rules gives 90,100 (BR-80, AT-12)`() {
        val movements = listOf(
            tx(INCOME, lkr(120_000), at("2026-10-25")),
            tx(EXPENSE, lkr(40_000), at("2026-10-26")),
            tx(TRANSFER, lkr(39_900), at("2026-10-25"), to = "goal-fund"),
        )
        assertEquals(lkr(90_100), balances.balanceOf(bank, movements))
    }

    @Test
    fun `ATM withdrawal moves money from bank to cash and is not spending (AT-15, BR-13)`() {
        val cash = account("cash", AccountKind.CASH, checkpoint = 0L)
        val atm = tx(TRANSFER, lkr(10_000), at("2026-10-10"), account = "bank-main", to = "cash")

        val result = balances(listOf(bank, cash), listOf(atm)).associate { it.account.id to it.balanceMinor }

        assertEquals(lkr(40_000), result["bank-main"])
        assertEquals(lkr(10_000), result["cash"])
    }

    @Test
    fun `movements on or before the checkpoint are already inside it (BR-32, BR-83)`() {
        val before = tx(EXPENSE, lkr(5_000), at("2026-09-30"))
        assertEquals(lkr(50_000), balances.balanceOf(bank, listOf(before)))
    }

    @Test
    fun `archived transactions never count (BR-16)`() {
        val undone = tx(EXPENSE, lkr(1_200), at("2026-10-05"), archived = true)
        assertEquals(lkr(50_000), balances.balanceOf(bank, listOf(undone)))
    }

    @Test
    fun `a refund comes back into the account (BR-14)`() {
        val movements = listOf(
            tx(EXPENSE, lkr(5_000), at("2026-10-05"), category = "shopping"),
            tx(REFUND, lkr(2_000), at("2026-10-07"), category = "shopping"),
        )
        assertEquals(lkr(47_000), balances.balanceOf(bank, movements))
    }

    @Test
    fun `other accounts' movements leave this one alone`() {
        val elsewhere = tx(EXPENSE, lkr(3_000), at("2026-10-05"), account = "cash")
        assertEquals(lkr(50_000), balances.balanceOf(bank, listOf(elsewhere)))
    }

    @Test
    fun `net change of the goal fund counts transfers in and out inside the window (BR-73)`() {
        val movements = listOf(
            tx(TRANSFER, lkr(39_900), at("2026-10-25"), to = "goal-fund"),
            tx(TRANSFER, lkr(5_000), at("2026-10-28"), account = "goal-fund", to = "bank-main"),
            tx(TRANSFER, lkr(10_000), at("2026-08-01"), to = "goal-fund"), // before the window
        )
        val net = balances.netChange("goal-fund", movements, from = at("2026-10-01"), to = at("2026-11-01"))
        assertEquals(lkr(34_900), net)
    }
}
