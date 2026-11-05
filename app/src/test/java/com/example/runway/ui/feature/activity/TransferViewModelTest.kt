package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = mockk<AccountRepository>()
    private val transactions = mockk<TransactionRepository>()
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), ZoneId.of("Asia/Colombo")))
    private val written = mutableListOf<Transaction>()

    private fun TestScope.viewModel(): TransferViewModel {
        every { accounts.observeAccounts() } returns flowOf(
            listOf(account("fund", AccountKind.GOAL_FUND), account("bank"), account("cash", AccountKind.CASH)),
        )
        coEvery { transactions.add(capture(written)) } returns AppResult.Success(Unit)
        return TransferViewModel(accounts, AddTransaction(transactions, time), time, SavedStateHandle()).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.accounts.collect {} }
        }
    }

    @Test
    fun `an ATM withdrawal is one transfer from bank to cash by default (BR-13, AT-15)`() = runTest {
        val vm = viewModel()
        vm.onAmountChange("10000")
        vm.onSave()

        val tx = written.single()
        assertEquals(TransactionType.TRANSFER, tx.type)
        assertEquals("bank", tx.accountId)
        assertEquals("cash", tx.toAccountId)
        assertEquals(1_000_000L, tx.amountLkrMinor)
    }

    @Test
    fun `a double tap writes the same document (BR-19)`() = runTest {
        val vm = viewModel()
        vm.onAmountChange("100")
        vm.onSave()
        vm.onSave()
        assertEquals(1, written.map { it.id }.toSet().size)
    }

    @Test
    fun `an invalid amount shows an inline error and writes nothing`() = runTest {
        val vm = viewModel()
        vm.onAmountChange("abc")
        vm.onSave()
        assertTrue(vm.form.value.amountError)
        assertTrue(written.isEmpty())
    }
}
