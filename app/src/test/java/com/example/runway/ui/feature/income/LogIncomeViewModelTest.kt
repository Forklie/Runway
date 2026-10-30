package com.example.runway.ui.feature.income

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.LogIncome
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.source
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
class LogIncomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sources = mockk<IncomeSourceRepository>()
    private val accounts = mockk<AccountRepository>()
    private val profile = mockk<UserProfileRepository>()
    private val transactions = mockk<TransactionRepository>()
    private val now = Instant.parse("2026-11-10T04:00:00Z")
    private val time = TimeProvider(Clock.fixed(now, ZoneId.of("Asia/Colombo")))
    private val batches = mutableListOf<List<Transaction>>()

    private val salary = source("salary", IncomeSourceKind.SALARY, Reliability.RELIABLE)
    private val adsense = source("adsense", IncomeSourceKind.ADSENSE, Reliability.VARIABLE).copy(currency = CurrencyCode.USD)

    private fun userProfile(pyf: Double = 30.0, rates: Map<String, Double> = emptyMap()) =
        UserProfile(uid = "u", displayName = "", email = "", pyfPercent = pyf, lastFxRates = rates, createdAt = now)

    @Before
    fun setUp() {
        every { sources.observeSources() } returns flowOf(listOf(salary, adsense))
        every { accounts.observeAccounts() } returns flowOf(listOf(account("fund", AccountKind.GOAL_FUND), account("bank")))
        every { profile.observeProfile() } returns flowOf(userProfile())
        coEvery { profile.setLastFxRate(any(), any()) } returns AppResult.Success(Unit)
        coEvery { transactions.addAll(capture(batches)) } returns AppResult.Success(Unit)
    }

    private fun TestScope.viewModel(saved: SavedStateHandle = SavedStateHandle()) = LogIncomeViewModel(
        sources = sources,
        accounts = accounts,
        profile = profile,
        logIncome = LogIncome(AddTransaction(transactions, time), transactions, profile),
        time = time,
        savedState = saved,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.options.collect {} } }

    @Test
    fun `AdSense USD 120 received as LKR 36,600 stores rate 305 (BR-06, AT-07)`() = runTest {
        val vm = viewModel()
        vm.onSourceChange("adsense")
        vm.onAmountChange("120")
        vm.onUseReceivedChange(true)
        vm.onReceivedChange("36,600")
        vm.onSave()

        val tx = batches.single().single()
        assertEquals(CurrencyCode.USD, tx.currency)
        assertEquals(305.0, tx.fxRate, 0.0)
        assertEquals(12_000L, tx.amountMinor)
        assertEquals(3_660_000L, tx.amountLkrMinor)
        assertEquals("adsense", tx.incomeSourceId)
        assertEquals("bank", tx.accountId)
        coVerify { profile.setLastFxRate(CurrencyCode.USD, 305.0) }
    }

    @Test
    fun `the last rate is the default for USD (BR-06)`() = runTest {
        every { profile.observeProfile() } returns flowOf(userProfile(rates = mapOf("USD" to 300.5)))
        val vm = viewModel()
        vm.onSourceChange("adsense")
        vm.onAmountChange("10")
        vm.onSave()
        assertEquals(300.5, batches.single().single().fxRate, 0.0)
    }

    @Test
    fun `USD with no rate shows an inline error and writes nothing`() = runTest {
        val vm = viewModel()
        vm.onSourceChange("adsense")
        vm.onAmountChange("120")
        vm.onSave()
        assertTrue(vm.form.value.rateError)
        assertEquals("120", vm.form.value.amount)
        assertTrue(batches.isEmpty())
    }

    @Test
    fun `salary with PYF at 30 percent is income plus a goal fund transfer in one batch (BR-33, AT-08)`() = runTest {
        val vm = viewModel()
        vm.onAmountChange("120000")
        vm.onSave()

        val (income, transfer) = batches.single()
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals(12_000_000L, income.amountLkrMinor)
        assertEquals(TransactionType.TRANSFER, transfer.type)
        assertEquals(3_600_000L, transfer.amountLkrMinor)
        assertEquals("bank", transfer.accountId)
        assertEquals("fund", transfer.toAccountId)
        assertEquals("${income.id}-pyf", transfer.id)
        assertEquals(income.date, transfer.date)
    }

    @Test
    fun `PYF can be switched off, and is never offered for other sources (FR-12)`() = runTest {
        val vm = viewModel()
        val options = (vm.options.value as UiState.Success).data
        assertTrue(options.offersPyf(vm.form.value))
        vm.onSourceChange("adsense")
        assertFalse(options.offersPyf(vm.form.value))

        vm.onSourceChange("salary")
        vm.onPyfChange(false)
        vm.onAmountChange("120000")
        vm.onSave()
        assertEquals(1, batches.single().size)
    }

    @Test
    fun `a double tap writes the same ids, back-dating moves the month (BR-18, BR-19)`() = runTest {
        val vm = viewModel()
        vm.onSourceChange("adsense")
        vm.onRateChange("305")
        vm.onAmountChange("100")
        vm.onDateChange(LocalDate.of(2026, 10, 31))
        vm.onSave()
        vm.onSave()
        assertEquals(1, batches.size)
        assertEquals("2026-10", batches.single().single().monthKey)
    }

    @Test
    fun `typed input comes back after process death`() = runTest {
        val saved = SavedStateHandle()
        val vm = viewModel(saved)
        vm.onSourceChange("adsense")
        vm.onAmountChange("120")
        vm.onRateChange("305")

        val restored = viewModel(saved).form.value
        assertEquals("adsense", restored.sourceId)
        assertEquals("120", restored.amount)
        assertEquals("305", restored.rate)
    }
}
