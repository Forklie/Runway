package com.example.runway.ui.feature.quickadd

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.CategoryKind.DISCRETIONARY
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.category
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
class QuickAddViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val categories = mockk<CategoryRepository>()
    private val accounts = mockk<AccountRepository>()
    private val profile = mockk<UserProfileRepository>()
    private val transactions = mockk<TransactionRepository>()
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), ZoneId.of("Asia/Colombo")))
    private val written = mutableListOf<Transaction>()

    @Before
    fun setUp() {
        every { categories.observeCategories() } returns flowOf(listOf(category("food", DISCRETIONARY, name = "Food")))
        every { accounts.observeAccounts() } returns flowOf(listOf(account("bank"), account("cash"), account("fund", AccountKind.GOAL_FUND)))
        every { profile.observeProfile() } returns flowOf(null)
        every { transactions.observeSince(any()) } returns flowOf(emptyList())
        coEvery { transactions.add(capture(written)) } returns AppResult.Success(Unit)
        coEvery { transactions.archive(any(), any()) } returns AppResult.Success(Unit)
    }

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) = QuickAddViewModel(
        categories = categories,
        accounts = accounts,
        profile = profile,
        transactions = transactions,
        addTransaction = AddTransaction(transactions, time),
        time = time,
        savedState = saved,
    )

    private fun TestScope.loaded(saved: SavedStateHandle = SavedStateHandle()) = viewModel(saved).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.options.collect {} }
        vm.onOpen()
    }

    private fun QuickAddViewModel.fill() {
        onAmountChange("1,250.50")
        onCategoryChange("food")
    }

    @Test
    fun `three taps write one expense with today's date and the default account (FR-07, FR-08)`() = runTest {
        val vm = loaded()
        vm.fill()
        vm.onSave()

        val tx = written.single()
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(125_050L, tx.amountLkrMinor)
        assertEquals(1.0, tx.fxRate, 0.0)
        assertEquals("food", tx.categoryId)
        assertEquals("bank", tx.accountId)
        assertEquals(time.now(), tx.date)
        assertFalse(vm.form.value.open)
        assertEquals("", vm.form.value.amount)
    }

    @Test
    fun `invalid amounts show an inline error, keep the input and write nothing (AT-03, AT-04)`() = runTest {
        val vm = loaded()
        vm.onCategoryChange("food")
        for (bad in listOf("abc", "0", "-5", "12.345")) {
            vm.onAmountChange(bad)
            vm.onSave()
            assertTrue(bad, vm.form.value.amountError)
            assertFalse(bad, vm.form.value.canSave)
            assertEquals(bad, vm.form.value.amount)
            assertTrue(bad, vm.form.value.open)
        }
        coVerify(exactly = 0) { transactions.add(any()) }
    }

    @Test
    fun `an empty amount is not an error yet, but cannot be saved`() = runTest {
        val vm = loaded()
        vm.onCategoryChange("food")
        assertFalse(vm.form.value.amountError)
        assertFalse(vm.form.value.canSave)
    }

    @Test
    fun `a double tap on Save writes exactly one document (AT-05)`() = runTest {
        val vm = loaded()
        vm.fill()
        vm.onSave()
        vm.onSave()
        assertEquals(1, written.size)
    }

    @Test
    fun `each new sheet gets its own client id (BR-19)`() = runTest {
        val vm = loaded()
        vm.fill()
        vm.onSave()
        vm.onOpen()
        vm.fill()
        vm.onSave()
        assertEquals(2, written.map { it.id }.toSet().size)
    }

    @Test
    fun `Undo archives the saved expense (AT-06)`() = runTest {
        val vm = loaded()
        vm.events.test {
            vm.fill()
            vm.onSave()
            val saved = awaitItem() as QuickAddEvent.Saved
            assertEquals(125_050L, saved.amountMinor)
            assertEquals("Food", saved.categoryName)
            vm.onUndo(saved.transactionId)
            coVerify { transactions.archive(saved.transactionId, true) }
        }
    }

    @Test
    fun `typed input and the open sheet come back after process death (AT-21)`() = runTest {
        val saved = SavedStateHandle()
        val vm = loaded(saved)
        vm.fill()
        vm.onNoteChange("kottu")
        vm.onDateChange(LocalDate.of(2026, 11, 2))

        val restored = viewModel(saved).form.value
        assertTrue(restored.open)
        assertEquals("1,250.50", restored.amount)
        assertEquals("food", restored.categoryId)
        assertEquals("kottu", restored.note)
        assertEquals(LocalDate.of(2026, 11, 2), restored.date)
    }

    @Test
    fun `a failed write reopens the sheet with the input kept`() = runTest {
        coEvery { transactions.add(any()) } returns AppResult.Failure(AppError.PermissionDenied)
        val vm = loaded()
        vm.events.test {
            vm.fill()
            vm.onSave()
            assertEquals(QuickAddEvent.Failed(AppError.PermissionDenied), awaitItem())
        }
        assertTrue(vm.form.value.open)
        assertEquals("1,250.50", vm.form.value.amount)
    }

    @Test
    fun `back-dating moves the month, a future date falls back to today (BR-18, BR-20)`() = runTest {
        val vm = loaded()
        vm.fill()
        vm.onDateChange(LocalDate.of(2026, 10, 31))
        vm.onSave()
        assertEquals("2026-10", written.single().monthKey)

        vm.onDateChange(LocalDate.of(2026, 12, 1))
        assertEquals(null, vm.form.value.date)
    }

    @Test
    fun `the default account is the last one used, and never the goal fund`() = runTest {
        every { transactions.observeSince(any()) } returns flowOf(
            listOf(
                tx(TransactionType.TRANSFER, lkr(100), at("2026-11-09"), account = "bank"),
                tx(TransactionType.EXPENSE, lkr(100), at("2026-11-08"), account = "cash"),
            ),
        )
        val vm = loaded()
        val options = (vm.options.value as UiState.Success).data
        assertEquals("cash", options.defaultAccountId)
        assertEquals(listOf("bank", "cash"), options.accounts.map { it.id })
    }

    @Test
    fun `no categories is the empty state, a listener failure is the error state`() = runTest {
        every { categories.observeCategories() } returns flowOf(emptyList())
        assertEquals(UiState.Empty, loaded().options.value)

        every { categories.observeCategories() } returns kotlinx.coroutines.flow.flow { throw IllegalStateException("signed out") }
        assertTrue(loaded().options.value is UiState.Error)
    }

    @Test
    fun `importing a bank SMS opens the sheet with the amount and saves nothing`() {
        val vm = viewModel()
        vm.onImport(165_000)
        assertEquals("1650.00", vm.form.value.amount)
        assertTrue(vm.form.value.open)
        assertFalse(vm.form.value.canSave) // still needs a category
    }

    @Test
    fun `an invalid import amount is ignored`() {
        val vm = viewModel()
        listOf(0L, -500L, Long.MAX_VALUE).forEach(vm::onImport)
        assertFalse(vm.form.value.open)
        assertEquals("", vm.form.value.amount)
    }
}
