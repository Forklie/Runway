package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CategoryKind.DISCRETIONARY
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.category
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.Fixtures.tx
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = mockk<TransactionRepository>()
    private val categories = mockk<CategoryRepository>()
    private val accounts = mockk<AccountRepository>()
    private val sources = mockk<IncomeSourceRepository>()
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), ZoneId.of("Asia/Colombo")))

    private val food = tx(TransactionType.EXPENSE, lkr(500), at("2026-11-09"), category = "food", id = "food")
    private val rent = tx(TransactionType.EXPENSE, lkr(900), at("2026-11-09"), category = "rent", id = "rent")
    private val older = tx(TransactionType.EXPENSE, lkr(100), at("2026-11-02"), category = "food", id = "older")

    @Before
    fun setUp() {
        every { transactions.observeMonth(any()) } returns flowOf(emptyList())
        every { transactions.observeMonth("2026-11") } returns flowOf(listOf(food, rent, older))
        every { categories.observeCategories() } returns flowOf(listOf(category("food", DISCRETIONARY), category("rent", DISCRETIONARY), category("unused", DISCRETIONARY)))
        every { accounts.observeAccounts() } returns flowOf(emptyList())
        every { sources.observeSources() } returns flowOf(emptyList())
    }

    private fun TestScope.viewModel() = ActivityViewModel(transactions, categories, accounts, sources, time, SavedStateHandle()).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun ActivityViewModel.content() = (uiState.value as UiState.Success).data

    @Test
    fun `the month is grouped by day with each day's spending, newest first (F-08)`() = runTest {
        val days = viewModel().content().days
        assertEquals(listOf("2026-11-09", "2026-11-02"), days.map { it.date.toString() })
        assertEquals(lkr(1400), days[0].spentMinor)
    }

    @Test
    fun `the filter offers only used categories and narrows the list`() = runTest {
        val vm = viewModel()
        assertEquals(listOf("food", "rent"), vm.content().filters.map { it.id })
        vm.onCategorySelected("rent")
        assertEquals(listOf("rent"), vm.content().days.flatMap { it.transactions }.map { it.id })
    }

    @Test
    fun `next month stops at the current month, an earlier month with nothing is the empty state`() = runTest {
        val vm = viewModel()
        assertFalse(vm.canGoNext(vm.month.value))
        vm.onNextMonth()
        assertEquals("2026-11", vm.month.value)

        vm.onPreviousMonth()
        assertEquals("2026-10", vm.month.value)
        assertEquals(UiState.Empty, vm.uiState.value)
        assertTrue(vm.canGoNext(vm.month.value))
    }

    @Test
    fun `a listener failure is the error state`() = runTest {
        every { transactions.observeMonth("2026-11") } returns flow { throw IllegalStateException("signed out") }
        assertTrue(viewModel().uiState.value is UiState.Error)
    }
}
