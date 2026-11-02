package com.example.runway.domain.usecase

import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddTransactionTest {

    private val now = Instant.parse("2026-11-03T04:00:00Z")
    private val time = TimeProvider(Clock.fixed(now, ZoneId.of("Asia/Colombo")))
    private val repository = mockk<TransactionRepository>()
    private val written = slot<Transaction>()
    private val addTransaction = AddTransaction(repository, time)

    @Before
    fun setUp() {
        coEvery { repository.add(capture(written)) } returns AppResult.Success(Unit)
    }

    private fun expense(amountMinor: Long = 120_000L, note: String = "") = TransactionDraft(
        id = "tx-1",
        type = TransactionType.EXPENSE,
        amountMinor = amountMinor,
        date = now,
        categoryId = "food-coffee",
        accountId = "cash",
        note = note,
    )

    @Test
    fun `an LKR expense keeps its amount and a rate of 1 (BR-03)`() = runTest {
        addTransaction(expense().copy(fxRate = 7.0)) // a stray rate is ignored for LKR

        assertEquals(1.0, written.captured.fxRate, 0.0)
        assertEquals(120_000L, written.captured.amountLkrMinor)
        assertEquals(now, written.captured.createdAt)
        assertEquals(now, written.captured.updatedAt)
    }

    @Test
    fun `USD 120 at 305 stores the rate and LKR 36,600 (AT-07)`() = runTest {
        addTransaction(
            TransactionDraft(
                id = "adsense-oct",
                type = TransactionType.INCOME,
                amountMinor = 12_000L,
                currency = CurrencyCode.USD,
                fxRate = 305.0,
                date = now,
                accountId = "bank-main",
                incomeSourceId = "adsense",
            ),
        )

        assertEquals(305.0, written.captured.fxRate, 0.0)
        assertEquals(3_660_000L, written.captured.amountLkrMinor)
    }

    @Test
    fun `monthKey follows Colombo time, not UTC (BR-20)`() = runTest {
        addTransaction(expense().copy(date = Instant.parse("2026-10-31T19:00:00Z")))
        assertEquals("2026-11", written.captured.monthKey)
    }

    @Test
    fun `the caller's id is the document id, so a double tap writes one document (BR-19, AT-05)`() = runTest {
        addTransaction(expense())
        addTransaction(expense())
        coVerify(exactly = 2) { repository.add(match { it.id == "tx-1" }) }
    }

    @Test
    fun `zero, negative and too large amounts are rejected before any write (AT-03)`() = runTest {
        listOf(0L, -500L, AddTransaction.MAX_MINOR + 1).forEach { amount ->
            val result = addTransaction(expense(amountMinor = amount))
            assertEquals(AppResult.Failure(AppError.Validation("amount", "out of range")), result)
        }
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `a note longer than the rules allow is rejected`() = runTest {
        val result = addTransaction(expense(note = "x".repeat(201)))
        assertTrue(result is AppResult.Failure)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `a transfer needs two different accounts (BR-13)`() = runTest {
        val sameAccount = TransactionDraft(
            id = "atm",
            type = TransactionType.TRANSFER,
            amountMinor = 1_000_000L,
            date = now,
            accountId = "cash",
            toAccountId = "cash",
        )
        assertTrue(addTransaction(sameAccount) is AppResult.Failure)
        assertTrue(addTransaction(sameAccount.copy(accountId = "bank-main")) is AppResult.Success)
    }

    @Test
    fun `a failed local write is passed back to the caller`() = runTest {
        coEvery { repository.add(any()) } returns AppResult.Failure(AppError.PermissionDenied)
        assertEquals(AppResult.Failure(AppError.PermissionDenied), addTransaction(expense()))
    }
}
