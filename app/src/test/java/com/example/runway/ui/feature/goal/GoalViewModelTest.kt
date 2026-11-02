package com.example.runway.ui.feature.goal

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.ComputeAccountBalances
import com.example.runway.domain.usecase.ObserveGoalRunway
import com.example.runway.domain.usecase.ProjectGoal
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GoalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val runway = previewGoalRunway() // pace 39,900 a month, fund "MacBook fund"
    private val observeRunway = mockk<ObserveGoalRunway>()
    private val transactions = mockk<TransactionRepository>()
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), ZoneId.of("Asia/Colombo")))
    private val written = slot<Transaction>()

    @Before
    fun setUp() {
        every { observeRunway() } returns flowOf(runway)
        coEvery { transactions.add(capture(written)) } returns AppResult.Success(Unit)
        coEvery { transactions.archive(any(), any()) } returns AppResult.Success(Unit)
    }

    private fun viewModel() = GoalViewModel(
        observeRunway = observeRunway,
        projectGoal = ProjectGoal(ComputeAccountBalances()),
        addTransaction = AddTransaction(transactions, time),
        transactions = transactions,
        time = time,
        savedState = SavedStateHandle(),
    )

    @Test
    fun `a price that is not a number stays in the field with an inline error (AT-03)`() {
        val vm = viewModel()
        vm.onOpenWhatIf()
        vm.onWhatIfAmountChange("abc")
        vm.onSubmitWhatIf()

        assertTrue(vm.forms.value.whatIfAmountError)
        assertEquals("abc", vm.forms.value.whatIfAmount)
        assertEquals(GoalSheet.WhatIf, vm.forms.value.sheet)
    }

    @Test
    fun `what-if answers in days from the pace and writes nothing (BR-75)`() = runTest {
        val vm = viewModel()
        vm.whatIf.test {
            assertNull(awaitItem())
            vm.onOpenWhatIf()
            vm.onWhatIfLabelChange("Headphones")
            vm.onWhatIfAmountChange("12500")
            vm.onSubmitWhatIf()
            assertEquals(WhatIfResult("Headphones", 1_250_000, 9), awaitItem()) // 12,500 ÷ (39,900 ÷ 30)
            vm.onDismissWhatIf()
            assertNull(awaitItem())
        }
        coVerify(exactly = 0) { transactions.add(any()) }
    }

    @Test
    fun `add to fund writes one transfer into the goal fund, even on a double tap (BR-13, BR-19)`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onOpenAddToFund()
        vm.onFundAmountChange("5000")
        vm.onSubmitAddToFund()
        vm.onSubmitAddToFund() // second tap while the sheet closes

        val event = vm.events.first() as GoalEvent.AddedToFund
        coVerify(exactly = 1) { transactions.add(any()) }
        assertEquals(TransactionType.TRANSFER, written.captured.type)
        assertEquals("bank-main", written.captured.accountId)
        assertEquals("goal-fund", written.captured.toAccountId)
        assertEquals(500_000L, written.captured.amountLkrMinor)
        assertEquals(written.captured.id, event.transactionId)
        assertNull(vm.forms.value.sheet)
    }

    @Test
    fun `undo archives the transfer instead of deleting it (BR-16, BR-17)`() = runTest {
        val vm = viewModel()
        vm.onUndoAddToFund("tx-1")
        coVerify { transactions.archive("tx-1", true) }
    }

    @Test
    fun `add to fund without an amount asks for one`() = runTest {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onOpenAddToFund()
        vm.onSubmitAddToFund()

        assertTrue(vm.forms.value.fundAmountError)
        coVerify(exactly = 0) { transactions.add(any()) }
    }
}
