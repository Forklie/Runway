package com.example.runway.ui.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.OnboardingIncomeKind
import com.example.runway.domain.model.OnboardingPlan
import com.example.runway.domain.repository.OnboardingRepository
import com.example.runway.domain.usecase.ComputeDayZeroPreview
import com.example.runway.domain.usecase.Feasibility
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<OnboardingRepository>()

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        OnboardingViewModel(repository, ComputeDayZeroPreview(), saved)

    private fun OnboardingViewModel.fillGoal(name: String = "MacBook", target: String = "490000") {
        onGoalNameChange(name)
        onGoalTargetChange(target)
    }

    /** Walks to the preview with the Doc 1 numbers. */
    private fun OnboardingViewModel.reachPreview() {
        fillGoal()
        onNext()                          // -> Income
        onSalaryBaseChange("120000")
        onNext()                          // -> Accounts
        onGoalFundBalanceChange("11200")
        onNext()                          // -> Committed
        onRentChange("34000")
        onNext()                          // -> Preview
    }

    // ---- step 1: the goal is the only required step ----

    @Test
    fun `starts on the goal step with a 12 month default`() {
        val state = viewModel().state.value
        assertEquals(OnboardingStep.Goal, state.step)
        assertEquals(12, state.goalMonths)
    }

    @Test
    fun `next without a name or target shows both errors and stays put`() {
        val vm = viewModel()
        vm.onNext()

        assertEquals(OnboardingStep.Goal, vm.state.value.step)
        assertEquals(setOf(OnboardingField.GOAL_NAME, OnboardingField.GOAL_TARGET), vm.state.value.fieldErrors)
    }

    @Test
    fun `a target of zero or with three decimals is rejected (BR-07)`() {
        val vm = viewModel()
        vm.fillGoal(target = "0")
        vm.onNext()
        assertTrue(OnboardingField.GOAL_TARGET in vm.state.value.fieldErrors)

        vm.onGoalTargetChange("100.123")
        vm.onNext()
        assertTrue(OnboardingField.GOAL_TARGET in vm.state.value.fieldErrors)
    }

    @Test
    fun `a valid goal moves to income and keeps what was typed`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()

        assertEquals(OnboardingStep.Income, vm.state.value.step)
        assertEquals("MacBook", vm.state.value.goalName)
        assertTrue(vm.state.value.fieldErrors.isEmpty())
    }

    @Test
    fun `typing clears that field's error`() {
        val vm = viewModel()
        vm.onNext()
        vm.onGoalNameChange("MacBook")

        assertFalse(OnboardingField.GOAL_NAME in vm.state.value.fieldErrors)
        assertTrue(OnboardingField.GOAL_TARGET in vm.state.value.fieldErrors)
    }

    @Test
    fun `the goal step cannot be skipped`() {
        val vm = viewModel()
        vm.onSkip()
        assertEquals(OnboardingStep.Goal, vm.state.value.step)
    }

    // ---- steps 2 to 4: optional ----

    @Test
    fun `an empty income step is fine`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()
        vm.onNext()
        assertEquals(OnboardingStep.Accounts, vm.state.value.step)
    }

    @Test
    fun `an invalid optional amount is an inline error and keeps the text`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()
        vm.onSalaryBaseChange("12.345")
        vm.onNext()

        assertEquals(OnboardingStep.Income, vm.state.value.step)
        assertEquals(setOf(OnboardingField.SALARY_BASE), vm.state.value.fieldErrors)
        assertEquals("12.345", vm.state.value.salaryBase)
    }

    @Test
    fun `a typed zero is allowed for an opening balance`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()
        vm.onNext()
        vm.onCashBalanceChange("0")
        vm.onNext()
        assertEquals(OnboardingStep.Committed, vm.state.value.step)
    }

    @Test
    fun `pay day must be between 1 and 31`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()
        vm.onSalaryDayChange("32")
        vm.onNext()
        assertEquals(setOf(OnboardingField.SALARY_DAY), vm.state.value.fieldErrors)

        vm.onSalaryDayChange("0")
        vm.onNext()
        assertEquals(setOf(OnboardingField.SALARY_DAY), vm.state.value.fieldErrors)

        vm.onSalaryDayChange("25")
        vm.onNext()
        assertEquals(OnboardingStep.Accounts, vm.state.value.step)
    }

    @Test
    fun `skip clears the step and moves on, even with a broken number`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()
        vm.onSalaryBaseChange("12.345")
        vm.onToggleVariableSource(OnboardingIncomeKind.ADSENSE)
        vm.onSkip()

        val state = vm.state.value
        assertEquals(OnboardingStep.Accounts, state.step)
        assertEquals("", state.salaryBase)
        assertTrue(state.variableSources.isEmpty())
    }

    @Test
    fun `ticking a source twice unticks it`() {
        val vm = viewModel()
        vm.onToggleVariableSource(OnboardingIncomeKind.FREELANCE)
        assertEquals(setOf(OnboardingIncomeKind.FREELANCE), vm.state.value.variableSources)
        vm.onToggleVariableSource(OnboardingIncomeKind.FREELANCE)
        assertTrue(vm.state.value.variableSources.isEmpty())
    }

    @Test
    fun `back goes to the previous step without validating`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext()
        vm.onSalaryBaseChange("12.345")
        vm.onBack()

        assertEquals(OnboardingStep.Goal, vm.state.value.step)
        assertEquals("12.345", vm.state.value.salaryBase)
    }

    @Test
    fun `back on the first step does nothing`() {
        val vm = viewModel()
        vm.onBack()
        assertEquals(OnboardingStep.Goal, vm.state.value.step)
    }

    // ---- step 5: the day-zero preview ----

    @Test
    fun `preview shows LKR 39,900 a month for the worked example (AT-09)`() {
        val vm = viewModel()
        vm.reachPreview()

        val preview = vm.state.value.preview!!
        assertEquals(OnboardingStep.Preview, vm.state.value.step)
        assertEquals(3_990_000L, preview.requiredPerMonthMinor)
        assertEquals(33, preview.sharePercent)
        assertEquals(8_600_000L, preview.leftAfterRentMinor)
        assertEquals(Feasibility.FITS, preview.feasibility)
    }

    @Test
    fun `preview with everything skipped still works and says unknown`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onNext(); vm.onSkip(); vm.onSkip(); vm.onSkip()

        assertEquals(OnboardingStep.Preview, vm.state.value.step)
        assertEquals(Feasibility.UNKNOWN, vm.state.value.preview!!.feasibility)
        assertNull(vm.state.value.preview!!.sharePercent)
    }

    @Test
    fun `leaving the preview drops it`() {
        val vm = viewModel()
        vm.reachPreview()
        vm.onBack()
        assertNull(vm.state.value.preview)
    }

    // ---- finish ----

    @Test
    fun `finish sends one plan in minor units and stays busy`() {
        val captured = slot<OnboardingPlan>()
        coEvery { repository.complete(capture(captured)) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.reachPreview()
        vm.onNext() // Next on the last step is Finish

        val plan = captured.captured
        assertEquals("MacBook", plan.goalName)
        assertEquals(49_000_000L, plan.goalTargetMinor)
        assertEquals(12, plan.goalMonths)
        assertEquals(12_000_000L, plan.salaryBaseMinor)
        assertEquals(1_120_000L, plan.goalFundBalanceMinor)
        assertEquals(3_400_000L, plan.rentMinor)
        assertEquals(0L, plan.gymMinor)
        assertTrue(vm.state.value.submitting)
    }

    @Test
    fun `a failed write keeps the input and shows the error`() {
        coEvery { repository.complete(any()) } returns AppResult.Failure(AppError.Unknown())
        val vm = viewModel()
        vm.reachPreview()
        vm.onFinish()

        assertFalse(vm.state.value.submitting)
        assertEquals(AppError.Unknown(), vm.state.value.error)
        assertEquals("MacBook", vm.state.value.goalName)
        assertEquals(OnboardingStep.Preview, vm.state.value.step)
    }

    @Test
    fun `a double tap on finish sends one batch`() {
        val gate = CompletableDeferred<AppResult<Unit>>()
        coEvery { repository.complete(any()) } coAnswers { gate.await() }
        val vm = viewModel()
        vm.reachPreview()
        vm.onFinish()
        vm.onFinish()

        coVerify(exactly = 1) { repository.complete(any()) }
        gate.complete(AppResult.Failure(AppError.Network))
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun `finish cannot run before the preview step`() {
        val vm = viewModel()
        vm.fillGoal()
        vm.onFinish()
        coVerify(exactly = 0) { repository.complete(any()) }
    }

    // ---- process death ----

    @Test
    fun `the draft and the step come back from saved state`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.fillGoal()
        first.onGoalMonthsChange(18)
        first.onNext()
        first.onSalaryBaseChange("120000")
        first.onToggleVariableSource(OnboardingIncomeKind.ADSENSE)

        val restored = viewModel(saved).state.value
        assertEquals(OnboardingStep.Income, restored.step)
        assertEquals("MacBook", restored.goalName)
        assertEquals("490000", restored.goalTarget)
        assertEquals(18, restored.goalMonths)
        assertEquals("120000", restored.salaryBase)
        assertEquals(setOf(OnboardingIncomeKind.ADSENSE), restored.variableSources)
    }

    @Test
    fun `restoring on the preview step rebuilds the preview`() {
        val saved = SavedStateHandle()
        viewModel(saved).reachPreview()

        val restored = viewModel(saved).state.value
        assertEquals(OnboardingStep.Preview, restored.step)
        assertEquals(3_990_000L, restored.preview!!.requiredPerMonthMinor)
    }
}
