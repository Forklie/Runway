package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = mockk<TransactionRepository>()
    private val categories = mockk<CategoryRepository>()
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), ZoneId.of("Asia/Colombo")))
    private val updated = mutableListOf<Transaction>()
    private val added = mutableListOf<Transaction>()

    private val expense = tx(TransactionType.EXPENSE, lkr(5000), at("2026-11-02"), category = "food", id = "t1")
    private val adsense = tx(TransactionType.INCOME, 12_000, at("2026-11-02"), id = "t1")
        .copy(currency = CurrencyCode.USD, fxRate = 305.0, amountLkrMinor = 3_660_000)

    @Before
    fun setUp() {
        every { categories.observeCategories() } returns flowOf(emptyList())
        coEvery { transactions.update(capture(updated)) } returns AppResult.Success(Unit)
        coEvery { transactions.add(capture(added)) } returns AppResult.Success(Unit)
        coEvery { transactions.archive(any(), any()) } returns AppResult.Success(Unit)
    }

    private fun viewModel(stored: Transaction?): TransactionDetailViewModel {
        every { transactions.observeById("t1") } returns flowOf(stored)
        return TransactionDetailViewModel(transactions, categories, AddTransaction(transactions, time), time, SavedStateHandle(mapOf("id" to "t1")))
    }

    @Test
    fun `editing the amount keeps the stored rate (BR-05, AT-22)`() = runTest {
        val vm = viewModel(adsense)
        assertEquals("120", vm.form.value.amount)
        vm.onAmountChange("100")
        vm.onSave()

        val saved = updated.single()
        assertEquals(305.0, saved.fxRate, 0.0)
        assertEquals(10_000L, saved.amountMinor)
        assertEquals(3_050_000L, saved.amountLkrMinor)
        assertEquals(adsense.createdAt, saved.createdAt)
    }

    @Test
    fun `moving the date moves the month, a future date is ignored (BR-18, BR-20)`() = runTest {
        val vm = viewModel(expense)
        vm.onDateChange(LocalDate.of(2026, 12, 1))
        assertEquals(LocalDate.of(2026, 11, 2), vm.form.value.date)

        vm.onDateChange(LocalDate.of(2026, 10, 31))
        vm.onSave()
        assertEquals("2026-10", updated.single().monthKey)
    }

    @Test
    fun `an invalid amount shows an inline error and writes nothing (BR-07)`() = runTest {
        val vm = viewModel(expense)
        vm.onAmountChange("0")
        vm.onSave()
        assertTrue(vm.form.value.amountError)
        assertEquals("0", vm.form.value.amount)
        assertTrue(updated.isEmpty())
    }

    @Test
    fun `archive soft-deletes (BR-16)`() = runTest {
        viewModel(expense).onArchive()
        coVerify { transactions.archive("t1", true) }
    }

    @Test
    fun `a refund links to the expense and its category (BR-14, AT-16)`() = runTest {
        val vm = viewModel(expense)
        vm.onRefundOpen()
        vm.onRefundChange("2000")
        vm.onRefundSave()

        val refund = added.single()
        assertEquals(TransactionType.REFUND, refund.type)
        assertEquals("t1", refund.relatedTxId)
        assertEquals("food", refund.categoryId)
        assertEquals(lkr(2000), refund.amountLkrMinor)
    }

    @Test
    fun `a refund above the original is rejected`() = runTest {
        val vm = viewModel(expense)
        vm.onRefundOpen()
        vm.onRefundChange("5000.01")
        vm.onRefundSave()
        assertTrue(vm.form.value.refundError)
        assertTrue(added.isEmpty())
    }

    @Test
    fun `an archived or missing transaction is not found`() = runTest {
        assertTrue(viewModel(null).uiState.value is UiState.Error)
        assertTrue(viewModel(expense.copy(archived = true)).uiState.value is UiState.Error)
    }
}
