package com.example.runway.domain.usecase

import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.RecurringKind
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.RecurringRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Sri Lanka time, UTC+5:30, no daylight saving: 00:00 on a Colombo day is 18:30 UTC the day before. */
class GenerateRecurringTest {

    private val zone = ZoneId.of("Asia/Colombo")
    private val recurring = mockk<RecurringRepository>()
    private val transactions = mockk<TransactionRepository>()
    private val profiles = mockk<UserProfileRepository>()

    @Before
    fun setUp() {
        coEvery { transactions.addAll(any()) } returns AppResult.Success(Unit)
        coEvery { recurring.update(any()) } returns AppResult.Success(Unit)
        every { profiles.observeProfile() } returns flowOf(profile())
    }

    private fun useCase(today: String = "2026-10-30"): GenerateRecurring {
        // 10:00 Colombo on the given day
        val clock = Clock.fixed(LocalDate.parse(today).atTime(10, 0).atZone(zone).toInstant(), zone)
        return GenerateRecurring(recurring, transactions, profiles, TimeProvider(clock))
    }

    private fun midnight(day: String): Instant = LocalDate.parse(day).atStartOfDay(zone).toInstant()

    private fun profile(rates: Map<String, Double> = emptyMap()) =
        UserProfile(uid = "u1", displayName = "K", email = "k@example.com", lastFxRates = rates, createdAt = Instant.EPOCH)

    private fun rent(
        dueDay: String,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        currency: CurrencyCode = CurrencyCode.LKR,
        amount: Long = 3_400_000,
    ) = RecurringItem(
        id = "recurring-rent", kind = RecurringKind.RENT, name = "Rent", amountMinor = amount,
        currency = currency, frequency = frequency, nextDueDate = midnight(dueDay),
        categoryId = "rent", accountId = "bank-main", active = true,
    )

    private fun dueItems(vararg items: RecurringItem) {
        coEvery { recurring.getDue(any()) } returns AppResult.Success(items.toList())
    }

    private fun capturedTransactions(): List<Transaction> {
        val slot = slot<List<Transaction>>()
        coVerify { transactions.addAll(capture(slot)) }
        return slot.captured
    }

    private fun capturedUpdates(): List<RecurringItem> {
        val captured = mutableListOf<RecurringItem>()
        coVerify { recurring.update(capture(captured)) }
        return captured
    }

    // ---- the happy path (AT-17) ----

    @Test
    fun `an item due today becomes one expense with the deterministic id`() = runTest {
        dueItems(rent("2026-10-30"))
        val result = useCase().invoke()

        assertEquals(AppResult.Success(1), result)
        val tx = capturedTransactions().single()
        assertEquals("recurring-rent_20261030", tx.id)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(3_400_000L, tx.amountMinor)
        assertEquals(3_400_000L, tx.amountLkrMinor)
        assertEquals(CurrencyCode.LKR, tx.currency)
        assertEquals(1.0, tx.fxRate, 0.0)
        assertEquals("rent", tx.categoryId)
        assertEquals("bank-main", tx.accountId)
        assertEquals("Rent", tx.note)
        assertEquals("2026-10", tx.monthKey)
        assertEquals(false, tx.archived)
        assertEquals(midnight("2026-10-30"), tx.date)
    }

    @Test
    fun `the due date moves on one month (BR-91)`() = runTest {
        dueItems(rent("2026-10-30"))
        useCase().invoke()
        assertEquals(midnight("2026-11-30"), capturedUpdates().single().nextDueDate)
    }

    @Test
    fun `createdAt and updatedAt are the due date, so a rewrite never changes createdAt`() = runTest {
        dueItems(rent("2026-10-30"))
        useCase().invoke()
        val tx = capturedTransactions().single()
        assertEquals(tx.date, tx.createdAt)
        assertEquals(tx.date, tx.updatedAt)
    }

    @Test
    fun `running twice writes exactly the same transactions, so nothing is duplicated (AT-17)`() = runTest {
        dueItems(rent("2026-10-30"))
        val case = useCase()
        case.invoke()
        case.invoke()

        val batches = mutableListOf<List<Transaction>>()
        coVerify(exactly = 2) { transactions.addAll(capture(batches)) }
        assertEquals(batches[0], batches[1])
    }

    @Test
    fun `due items are asked for up to the very end of today`() = runTest {
        dueItems()
        useCase().invoke()
        // 23:59:59.999 Colombo = 18:29:59.999 UTC
        coVerify { recurring.getDue(Instant.parse("2026-10-30T18:29:59.999Z")) }
    }

    @Test
    fun `nothing due writes nothing`() = runTest {
        dueItems()
        assertEquals(AppResult.Success(0), useCase().invoke())
        coVerify(exactly = 0) { transactions.addAll(any()) }
        coVerify(exactly = 0) { recurring.update(any()) }
    }

    // ---- catching up and month ends ----

    @Test
    fun `missed months each get their own back-dated transaction`() = runTest {
        dueItems(rent("2026-08-30"))
        val result = useCase("2026-10-30").invoke()

        assertEquals(AppResult.Success(3), result)
        assertEquals(
            listOf("recurring-rent_20260830", "recurring-rent_20260930", "recurring-rent_20261030"),
            capturedTransactions().map { it.id },
        )
        assertEquals(listOf("2026-08", "2026-09", "2026-10"), capturedTransactions().map { it.monthKey })
        assertEquals(midnight("2026-11-30"), capturedUpdates().single().nextDueDate)
    }

    @Test
    fun `the 31st survives a short month`() = runTest {
        dueItems(rent("2026-01-31"))
        useCase("2026-03-31").invoke()

        assertEquals(
            listOf("recurring-rent_20260131", "recurring-rent_20260228", "recurring-rent_20260331"),
            capturedTransactions().map { it.id },
        )
        assertEquals(midnight("2026-04-30"), capturedUpdates().single().nextDueDate)
    }

    @Test
    fun `a yearly item moves on twelve months`() = runTest {
        dueItems(rent("2026-10-30", frequency = RecurringFrequency.YEARLY))
        useCase().invoke()
        assertEquals(midnight("2027-10-30"), capturedUpdates().single().nextDueDate)
    }

    @Test
    fun `a long gap is capped at 12 per run and picked up next time`() = runTest {
        dueItems(rent("2024-01-30"))
        val result = useCase("2026-10-30").invoke()

        assertEquals(AppResult.Success(12), result)
        assertEquals(midnight("2025-01-30"), capturedUpdates().single().nextDueDate)
    }

    // ---- currencies ----

    @Test
    fun `a USD item is converted with the profile rate and keeps its rate`() = runTest {
        every { profiles.observeProfile() } returns flowOf(profile(mapOf("USD" to 305.0)))
        dueItems(rent("2026-10-30", currency = CurrencyCode.USD, amount = 1_000))
        useCase().invoke()

        val tx = capturedTransactions().single()
        assertEquals(1_000L, tx.amountMinor)
        assertEquals(305.0, tx.fxRate, 0.0)
        assertEquals(305_000L, tx.amountLkrMinor)
    }

    @Test
    fun `a USD item with no rate is left for next time`() = runTest {
        dueItems(rent("2026-10-30", currency = CurrencyCode.USD, amount = 1_000))
        val result = useCase().invoke()

        assertEquals(AppResult.Success(0), result)
        coVerify(exactly = 0) { transactions.addAll(any()) }
        coVerify(exactly = 0) { recurring.update(any()) }
    }

    // ---- failures ----

    @Test
    fun `if the transactions cannot be written the due date does not move`() = runTest {
        dueItems(rent("2026-10-30"))
        coEvery { transactions.addAll(any()) } returns AppResult.Failure(AppError.Unknown())

        assertTrue(useCase().invoke() is AppResult.Failure)
        coVerify(exactly = 0) { recurring.update(any()) }
    }

    @Test
    fun `a failed read of the due items is returned as is`() = runTest {
        coEvery { recurring.getDue(any()) } returns AppResult.Failure(AppError.Network)
        assertEquals(AppResult.Failure(AppError.Network), useCase().invoke())
        coVerify(exactly = 0) { transactions.addAll(any()) }
    }
}
