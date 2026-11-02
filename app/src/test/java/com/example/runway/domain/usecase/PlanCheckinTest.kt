package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.GapResolution
import com.example.runway.domain.model.TransactionType
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.lkr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanCheckinTest {

    private val plan = PlanCheckin(ComputeUnaccounted())
    private val now = at("2026-11-08")
    private val bank = CheckinAccount(account("bank-main"), expectedMinor = lkr(90_100))
    private val cash = CheckinAccount(account("cash", AccountKind.CASH), expectedMinor = lkr(5_700))

    @Test
    fun `worked example records a 6,100 gap and the entered balance as the new checkpoint (AT-12)`() {
        val result = plan.plan("s1", listOf(EnteredBalance(bank, lkr(84_000))), logGapsAs = null, gapNote = "", at = now)
        val checkin = result.checkins.single()

        assertEquals(lkr(6_100), checkin.gapMinor)
        assertEquals(lkr(84_000), checkin.enteredBalanceMinor)
        assertEquals(now, checkin.date)
        assertEquals(GapResolution.UNACCOUNTED, checkin.resolution)
        assertTrue(result.gapExpenses.isEmpty())
    }

    @Test
    fun `logging the gap adds one expense dated just before the checkpoint (BR-84)`() {
        val result = plan.plan(
            "s1",
            listOf(EnteredBalance(bank, lkr(84_000)), EnteredBalance(cash, lkr(4_500))),
            logGapsAs = "other",
            gapNote = "Unlogged spending",
            at = now,
        )
        assertEquals(listOf(GapResolution.LOGGED, GapResolution.LOGGED), result.checkins.map { it.resolution })
        val expense = result.gapExpenses.first()
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals(lkr(6_100), expense.amountMinor)
        assertEquals("other", expense.categoryId)
        assertEquals("bank-main", expense.accountId)
        assertTrue(expense.date.isBefore(now))
        assertEquals(lkr(1_200), result.gapExpenses[1].amountMinor) // the cash check: 5,700 expected, 4,500 in the wallet
    }

    @Test
    fun `more money than expected is never logged as spending (BR-81)`() {
        val result = plan.plan("s1", listOf(EnteredBalance(cash, lkr(6_700))), logGapsAs = "other", gapNote = "", at = now)
        assertEquals(lkr(-1_000), result.checkins.single().gapMinor)
        assertEquals(GapResolution.UNACCOUNTED, result.checkins.single().resolution)
        assertTrue(result.gapExpenses.isEmpty())
    }

    @Test
    fun `a balance that matches needs no resolution`() {
        val result = plan.plan("s1", listOf(EnteredBalance(cash, lkr(5_700))), logGapsAs = null, gapNote = "", at = now)
        assertEquals(GapResolution.NONE, result.checkins.single().resolution)
    }

    @Test
    fun `ids come from the session, so saving twice cannot duplicate (BR-19)`() {
        val first = plan.plan("s1", listOf(EnteredBalance(bank, lkr(84_000))), "other", "", now)
        val again = plan.plan("s1", listOf(EnteredBalance(bank, lkr(84_000))), "other", "", now)
        assertEquals(first.checkins.single().id, again.checkins.single().id)
        assertEquals(first.gapExpenses.single().id, again.gapExpenses.single().id)
    }

    @Test
    fun `a balance can be zero but not negative or text`() {
        assertEquals(0L, plan.parseBalance("0"))
        assertEquals(0L, plan.parseBalance("0.00"))
        assertEquals(lkr(84_000), plan.parseBalance("84,000"))
        assertNull(plan.parseBalance("abc"))
        assertNull(plan.parseBalance("-5"))
        assertNull(plan.parseBalance("12.345"))
    }
}
