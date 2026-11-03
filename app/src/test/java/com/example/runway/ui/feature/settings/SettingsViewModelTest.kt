package com.example.runway.ui.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.GoalSettings
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.repository.GoalSettingsRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-10-30T04:30:00Z") // 10:00 in Colombo
    private val time = TimeProvider(Clock.fixed(now, ZoneId.of("Asia/Colombo")))

    private val profileFlow = MutableStateFlow<UserProfile?>(profile())
    private val goalFlow = MutableStateFlow<GoalSettings?>(goal())
    private val accountsFlow = MutableStateFlow(accounts())

    private val profiles = mockk<UserProfileRepository>()
    private val goals = mockk<GoalSettingsRepository>()
    private val accountRepo = mockk<AccountRepository>()
    private val auth = mockk<AuthRepository>()

    @Before
    fun setUp() {
        every { profiles.observeProfile() } returns profileFlow
        every { goals.observeGoal() } returns goalFlow
        every { accountRepo.observeAccounts() } returns accountsFlow
        coEvery { profiles.setDisplayName(any()) } returns AppResult.Success(Unit)
        coEvery { profiles.setPyfPercent(any()) } returns AppResult.Success(Unit)
        coEvery { profiles.setDefaultAccount(any()) } returns AppResult.Success(Unit)
        coEvery { goals.updateGoal(any(), any(), any()) } returns AppResult.Success(Unit)
        every { auth.signOut() } returns Unit
    }

    private fun viewModel() = SettingsViewModel(profiles, goals, accountRepo, auth, time)

    private fun profile(pyf: Double = 30.0) = UserProfile(
        uid = "u1", displayName = "Kavindu", email = "kavindu@example.com",
        pyfPercent = pyf, defaultAccountId = "bank-main", createdAt = Instant.EPOCH,
    )

    private fun goal() = GoalSettings(
        name = "MacBook", targetMinor = 49_000_000,
        startDate = Instant.parse("2026-10-30T00:00:00Z"), deadline = Instant.parse("2027-10-30T00:00:00Z"),
        fundAccountId = "goal-fund", active = true,
    )

    private fun accounts() = listOf(
        Account("bank-main", "Main bank", AccountKind.BANK, 0, 0, Instant.EPOCH, false, Instant.EPOCH),
        Account("cash", "Cash", AccountKind.CASH, 0, 0, Instant.EPOCH, false, Instant.EPOCH),
    )

    private suspend fun ReceiveTurbine<UiState<SettingsContent>>.awaitSuccess(): SettingsContent {
        while (true) {
            val item = awaitItem()
            if (item is UiState.Success) return item.data
        }
    }

    // ---- content ----

    @Test
    fun `shows profile, goal, accounts and PYF from the live listeners`() = runTest {
        viewModel().uiState.test {
            val content = awaitSuccess()
            assertEquals("Kavindu", content.displayName)
            assertEquals("kavindu@example.com", content.email)
            assertEquals("MacBook", content.goal?.name)
            assertEquals(2, content.accounts.size)
            assertEquals("bank-main", content.defaultAccountId)
            assertEquals(30.0, content.pyfPercent, 0.0)
        }
    }

    @Test
    fun `no profile yet means loading, not an error`() = runTest {
        profileFlow.value = null
        viewModel().uiState.test {
            assertEquals(UiState.Loading, awaitItem())
        }
    }

    @Test
    fun `a failing listener becomes an error state`() = runTest {
        every { goals.observeGoal() } returns kotlinx.coroutines.flow.flow { throw IllegalStateException("No signed-in user") }
        viewModel().uiState.test {
            var last = awaitItem()
            while (last !is UiState.Error) last = awaitItem()
            assertTrue((last as UiState.Error).error is AppError.Unknown)
        }
    }

    // ---- name ----

    @Test
    fun `saving the name trims it and clears the draft`() = runTest {
        val vm = viewModel()
        vm.onNameChange("  Kavindu S  ")
        vm.onSaveName()

        coVerify(exactly = 1) { profiles.setDisplayName("Kavindu S") }
        assertNull(vm.form.value.nameDraft)
    }

    @Test
    fun `a blank name is an inline error and nothing is written`() {
        val vm = viewModel()
        vm.onNameChange("   ")
        vm.onSaveName()

        assertTrue(vm.form.value.nameError)
        coVerify(exactly = 0) { profiles.setDisplayName(any()) }
    }

    @Test
    fun `saving with no edit does nothing`() {
        val vm = viewModel()
        vm.onSaveName()
        coVerify(exactly = 0) { profiles.setDisplayName(any()) }
    }

    // ---- PYF ----

    @Test
    fun `PYF accepts 0 to 100 and saves a Double percent`() {
        val vm = viewModel()
        vm.onPyfChange("30")
        vm.onSavePyf()
        coVerify(exactly = 1) { profiles.setPyfPercent(30.0) }
        assertNull(vm.form.value.pyfDraft)

        vm.onPyfChange("0")
        vm.onSavePyf()
        coVerify(exactly = 1) { profiles.setPyfPercent(0.0) }
    }

    @Test
    fun `PYF above 100 or not a number is an inline error`() {
        val vm = viewModel()
        vm.onPyfChange("101")
        vm.onSavePyf()
        assertTrue(vm.form.value.pyfError)

        vm.onPyfChange(".")
        vm.onSavePyf()
        assertTrue(vm.form.value.pyfError)
        coVerify(exactly = 0) { profiles.setPyfPercent(any()) }
    }

    @Test
    fun `typing in PYF keeps only digits and a dot`() {
        val vm = viewModel()
        vm.onPyfChange("3a0%")
        assertEquals("30", vm.form.value.pyfDraft)
    }

    // ---- default account ----

    @Test
    fun `choosing a default account saves it`() {
        viewModel().onDefaultAccountSelected("cash")
        coVerify(exactly = 1) { profiles.setDefaultAccount("cash") }
    }

    // ---- goal ----

    @Test
    fun `editing the goal opens the sheet with the saved values`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitSuccess()
            vm.onEditGoal()
        }
        val edit = vm.form.value.goalEdit!!
        assertEquals("MacBook", edit.name)
        assertEquals("490000", edit.target)
        assertNull(edit.months)
    }

    @Test
    fun `saving the goal without touching the deadline keeps it`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitSuccess()
            vm.onEditGoal()
        }
        vm.onGoalTargetChange("520000")
        vm.onSaveGoal()

        coVerify(exactly = 1) { goals.updateGoal("MacBook", 52_000_000L, null) }
        assertNull(vm.form.value.goalEdit)
    }

    @Test
    fun `choosing months moves the deadline from today`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitSuccess()
            vm.onEditGoal()
        }
        vm.onGoalMonthsChange(6)
        vm.onSaveGoal()

        // 30 Oct 2026 10:00 Colombo + 6 months = 30 Apr 2027 10:00 Colombo = 04:30 UTC
        coVerify { goals.updateGoal("MacBook", 49_000_000L, Instant.parse("2027-04-30T04:30:00Z")) }
    }

    @Test
    fun `tapping the same months twice goes back to keeping the deadline`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitSuccess()
            vm.onEditGoal()
        }
        vm.onGoalMonthsChange(6)
        vm.onGoalMonthsChange(6)
        assertNull(vm.form.value.goalEdit!!.months)
    }

    @Test
    fun `an empty name or a bad target keeps the sheet open with errors`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitSuccess()
            vm.onEditGoal()
        }
        vm.onGoalNameChange(" ")
        vm.onGoalTargetChange("0")
        vm.onSaveGoal()

        val edit = vm.form.value.goalEdit!!
        assertTrue(edit.nameError)
        assertTrue(edit.targetError)
        assertEquals("0", edit.target) // input kept
        coVerify(exactly = 0) { goals.updateGoal(any(), any(), any()) }
    }

    @Test
    fun `dismissing the sheet drops the edit`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitSuccess()
            vm.onEditGoal()
        }
        vm.onDismissGoal()
        assertNull(vm.form.value.goalEdit)
    }

    // ---- failures and sign out ----

    @Test
    fun `a save that cannot be queued shows an error and keeps the draft`() {
        coEvery { profiles.setDisplayName(any()) } returns AppResult.Failure(AppError.Unknown())
        val vm = viewModel()
        vm.onNameChange("Kavindu S")
        vm.onSaveName()

        assertEquals(AppError.Unknown(), vm.form.value.actionError)
        assertEquals("Kavindu S", vm.form.value.nameDraft)
    }

    @Test
    fun `sign out calls the repository and nothing else`() {
        viewModel().onSignOut()
        verify(exactly = 1) { auth.signOut() }
    }
}
