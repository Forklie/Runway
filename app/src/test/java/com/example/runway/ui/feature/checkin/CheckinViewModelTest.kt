package com.example.runway.ui.feature.checkin

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.model.GapResolution
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.repository.BalanceCheckinRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.CheckinAccount
import com.example.runway.domain.usecase.CheckinSetup
import com.example.runway.domain.usecase.ComputeUnaccounted
import com.example.runway.domain.usecase.ObserveCheckinSetup
import com.example.runway.domain.usecase.PlanCheckin
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.category
import com.example.runway.testutil.Fixtures.lkr
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-11-08T04:00:00Z")
    private val time = TimeProvider(Clock.fixed(now, ZoneId.of("Asia/Colombo")))
    private val setup = CheckinSetup(
        accounts = listOf(
            CheckinAccount(account("bank-main"), expectedMinor = lkr(90_100)),
            CheckinAccount(account("cash", AccountKind.CASH), expectedMinor = lkr(5_700)),
        ),
        categories = listOf(category("food-coffee", CategoryKind.DISCRETIONARY), category("other", CategoryKind.DISCRETIONARY)),
        lastCheckin = null,
    )
    private val observeSetup = mockk<ObserveCheckinSetup>()
    private val transactions = mockk<TransactionRepository>()
    private val checkins = mockk<BalanceCheckinRepository>()
    private val writtenTx = mutableListOf<Transaction>()
    private val savedCheckins = mutableListOf<List<BalanceCheckin>>()

    @Before
    fun setUp() {
        every { observeSetup() } returns flowOf(setup)
        coEvery { transactions.add(capture(writtenTx)) } returns AppResult.Success(Unit)
        coEvery { checkins.saveCheckin(capture(savedCheckins)) } returns AppResult.Success(Unit)
    }

    private fun TestScope.viewModel(): CheckinViewModel {
        val unaccounted = ComputeUnaccounted()
        val vm = CheckinViewModel(
            observeSetup = observeSetup,
            planCheckin = PlanCheckin(unaccounted),
            unaccounted = unaccounted,
            addTransaction = AddTransaction(transactions, time),
            checkins = checkins,
            time = time,
            savedState = SavedStateHandle(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.view.collect {} }
        return vm
    }

    @Test
    fun `a balance that is not a number keeps the step and the input (AT-03)`() = runTest {
        val vm = viewModel()
        vm.onBalanceChange("bank-main", "abc")
        vm.onContinue()

        assertTrue(vm.form.value.inputError)
        assertEquals(0, vm.form.value.step)
        assertEquals("abc", vm.form.value.entered["bank-main"])
    }

    @Test
    fun `the gap shows live as he types (BR-81)`() = runTest {
        val vm = viewModel()
        vm.onBalanceChange("bank-main", "84000")
        assertEquals(lkr(6_100), vm.view.value?.rows?.first()?.gapMinor)
        assertEquals(lkr(6_100), vm.view.value?.unloggedMinor)
    }

    @Test
    fun `worked example saves a 6,100 gap left as unaccounted, skipping cash (AT-12)`() = runTest {
        val vm = viewModel()
        vm.onBalanceChange("bank-main", "84000")
        vm.onContinue()
        vm.onSkipAccount()
        vm.onSave("Unlogged spending")
        vm.done.first()

        val saved = savedCheckins.single().single()
        assertEquals("bank-main", saved.accountId)
        assertEquals(lkr(6_100), saved.gapMinor)
        assertEquals(lkr(84_000), saved.enteredBalanceMinor)
        assertEquals(GapResolution.UNACCOUNTED, saved.resolution)
        coVerify(exactly = 0) { transactions.add(any()) }
    }

    @Test
    fun `logging the gap writes one expense per account, then the check-in (BR-84)`() = runTest {
        val vm = viewModel()
        vm.onBalanceChange("bank-main", "84000")
        vm.onContinue()
        vm.onBalanceChange("cash", "4500")
        vm.onContinue()
        vm.onLogGapsChange(true)
        vm.onSave("Unlogged spending")
        vm.done.first()

        assertEquals(listOf(lkr(6_100), lkr(1_200)), writtenTx.map { it.amountLkrMinor })
        assertEquals(setOf("other"), writtenTx.map { it.categoryId }.toSet())
        assertEquals("Unlogged spending", writtenTx.first().note)
        assertEquals(listOf(GapResolution.LOGGED, GapResolution.LOGGED), savedCheckins.single().map { it.resolution })
    }

    @Test
    fun `a second tap on Save writes nothing more (BR-19)`() = runTest {
        val vm = viewModel()
        vm.onBalanceChange("bank-main", "84000")
        vm.onContinue()
        vm.onSkipAccount()
        vm.onSave("Unlogged spending")
        vm.onSave("Unlogged spending")
        coVerify(exactly = 1) { checkins.saveCheckin(any()) }
    }

    @Test
    fun `Back walks through the steps and leaves from the first one`() = runTest {
        val vm = viewModel()
        vm.onBalanceChange("bank-main", "84000")
        vm.onContinue()
        assertTrue(vm.onStepBack())
        assertEquals(0, vm.form.value.step)
        assertEquals("84000", vm.form.value.entered["bank-main"]) // nothing typed is lost
        assertEquals(false, vm.onStepBack())
    }
}
