package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.model.MovementDirection
import com.example.runway.domain.repository.BankSmsRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountActivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sms = mockk<BankSmsRepository>()
    private val transactions = mockk<TransactionRepository> { every { observeMonth(any()) } returns flowOf(emptyList()) }
    private val colombo = ZoneId.of("Asia/Colombo")
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), colombo))
    private val debit = AccountMovement("1234", MovementDirection.OUT, MovementChannel.CARD, 500, Instant.parse("2026-11-09T04:00:00Z"))

    private fun TestScope.viewModel(): AccountActivityViewModel {
        val vm = AccountActivityViewModel(sms, transactions, time, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun start(key: String) = java.time.YearMonth.parse(key).atDay(1).atStartOfDay(colombo).toInstant()

    @Test
    fun `reads exactly the current month and summarizes it`() = runTest {
        coEvery { sms.movements(any(), any()) } returns listOf(debit)
        val state = viewModel().uiState.value as UiState.Success
        assertEquals(500L, state.data.accounts.single().outMinor)
        assertEquals(listOf(debit), state.data.toAdd)
        coVerify { sms.movements(start("2026-11"), start("2026-12")) }
    }

    @Test
    fun `no movements is empty`() = runTest {
        coEvery { sms.movements(any(), any()) } returns emptyList()
        assertEquals(UiState.Empty, viewModel().uiState.value)
    }

    @Test
    fun `a read failure is an error and retry reads again`() = runTest {
        coEvery { sms.movements(any(), any()) } throws SecurityException("no READ_SMS")
        val vm = viewModel()
        assertTrue(vm.uiState.value is UiState.Error)
        coEvery { sms.movements(any(), any()) } returns listOf(debit)
        vm.retry()
        assertTrue(vm.uiState.value is UiState.Success)
    }

    @Test
    fun `moving back reads that month, and the future is out of reach`() = runTest {
        coEvery { sms.movements(any(), any()) } returns emptyList()
        val vm = viewModel()
        assertFalse(vm.canGoNext(vm.month.value))
        vm.onNextMonth()
        assertEquals("2026-11", vm.month.value)
        vm.onPreviousMonth()
        assertEquals("2026-10", vm.month.value)
        assertTrue(vm.canGoNext(vm.month.value))
        coVerify { sms.movements(start("2026-10"), start("2026-11")) }
    }

    @Test
    fun `a ledger that fails to load still shows the measurements`() = runTest {
        coEvery { sms.movements(any(), any()) } returns listOf(debit)
        every { transactions.observeMonth(any()) } returns flow { throw IllegalStateException("signed out") }
        val state = viewModel().uiState.value as UiState.Success
        assertEquals(listOf(debit), state.data.toAdd)
    }
}
