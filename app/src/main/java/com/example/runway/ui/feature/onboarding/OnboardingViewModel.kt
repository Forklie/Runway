package com.example.runway.ui.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.OnboardingIncomeKind
import com.example.runway.domain.model.OnboardingPlan
import com.example.runway.domain.repository.OnboardingRepository
import com.example.runway.domain.usecase.ComputeDayZeroPreview
import com.example.runway.domain.usecase.DayZeroInput
import com.example.runway.domain.usecase.DayZeroPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One ViewModel for all five steps (FR-05, FR-06), so the draft lives in one place.
 * - Only the goal is required. Income, accounts and costs may be skipped (Skip clears that step).
 * - Validates on Next, never per keystroke, and keeps what the user typed.
 * - Every field and the current step go into SavedStateHandle, so rotation and process death keep the draft (AT-21 spirit).
 * - Finish writes everything in one batch. Nothing to navigate to: onboardingComplete flips, the session
 *   becomes Ready and RunwayShell moves the user Home.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    private val computeDayZeroPreview: ComputeDayZeroPreview,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    // ---- 1 Goal ----
    fun onGoalNameChange(value: String) = edit(OnboardingField.GOAL_NAME) { it.copy(goalName = value) }
    fun onGoalTargetChange(value: String) = edit(OnboardingField.GOAL_TARGET) { it.copy(goalTarget = value) }
    fun onGoalMonthsChange(months: Int) = change { it.copy(goalMonths = months) }

    // ---- 2 Income ----
    fun onSalaryBaseChange(value: String) = edit(OnboardingField.SALARY_BASE) { it.copy(salaryBase = value) }
    fun onSalaryDayChange(value: String) = edit(OnboardingField.SALARY_DAY) { it.copy(salaryDay = value.filter(Char::isDigit).take(2)) }
    fun onOwnEstimateChange(value: String) = edit(OnboardingField.OWN_ESTIMATE) { it.copy(ownEstimate = value) }
    fun onToggleVariableSource(kind: OnboardingIncomeKind) = change {
        it.copy(variableSources = if (kind in it.variableSources) it.variableSources - kind else it.variableSources + kind)
    }

    // ---- 3 Accounts ----
    fun onBankBalanceChange(value: String) = edit(OnboardingField.BANK) { it.copy(bankBalance = value) }
    fun onCashBalanceChange(value: String) = edit(OnboardingField.CASH) { it.copy(cashBalance = value) }
    fun onGoalFundBalanceChange(value: String) = edit(OnboardingField.GOAL_FUND) { it.copy(goalFundBalance = value) }

    // ---- 4 Committed costs ----
    fun onRentChange(value: String) = edit(OnboardingField.RENT) { it.copy(rent = value) }
    fun onUtilitiesChange(value: String) = edit(OnboardingField.UTILITIES) { it.copy(utilities = value) }
    fun onGymChange(value: String) = edit(OnboardingField.GYM) { it.copy(gym = value) }
    fun onSubscriptionsChange(value: String) = edit(OnboardingField.SUBSCRIPTIONS) { it.copy(subscriptions = value) }

    // ---- Navigation between steps ----

    /** Validates the current step. On the last step it finishes. */
    fun onNext() {
        val current = _state.value
        if (current.submitting) return
        if (current.step == OnboardingStep.Preview) {
            onFinish()
            return
        }
        val errors = validate(current.step, current)
        if (errors.isNotEmpty()) {
            change { it.copy(fieldErrors = errors) }
            return
        }
        goTo(current.step.next())
    }

    /** Back never validates: leaving a step must not be blocked by a half-typed number. */
    fun onBack() {
        val current = _state.value
        if (current.submitting || current.step == OnboardingStep.Goal) return
        goTo(current.step.previous())
    }

    /** Skip clears this step's fields, so what is on screen is exactly what gets saved. */
    fun onSkip() {
        val current = _state.value
        if (current.submitting || !current.step.skippable) return
        change {
            when (it.step) {
                OnboardingStep.Income -> it.copy(
                    salaryBase = "", salaryDay = OnboardingState.DEFAULT_PAY_DAY,
                    ownEstimate = "", variableSources = emptySet(),
                )
                OnboardingStep.Accounts -> it.copy(bankBalance = "", cashBalance = "", goalFundBalance = "")
                OnboardingStep.Committed -> it.copy(rent = "", utilities = "", gym = "", subscriptions = "")
                else -> it
            }
        }
        goTo(current.step.next())
    }

    /** One batch. On success we stay "busy": the session is about to change and the screen is replaced. */
    fun onFinish() {
        val current = _state.value
        if (current.submitting || current.step != OnboardingStep.Preview) return

        // Safety net after process death or a stale draft: send the user back to the first broken step.
        OnboardingStep.entries.forEach { step ->
            val errors = validate(step, current)
            if (errors.isNotEmpty()) {
                change { it.copy(step = step, fieldErrors = errors) }
                return
            }
        }

        change { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.complete(toPlan(current))) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> change { it.copy(submitting = false, error = result.error) }
            }
        }
    }

    // ---- State plumbing ----

    private fun goTo(step: OnboardingStep) = change {
        it.copy(
            step = step,
            fieldErrors = emptySet(),
            error = null,
            preview = if (step == OnboardingStep.Preview) previewFor(it) else null,
        )
    }

    /** An edit clears that field's error and any banner. */
    private fun edit(field: OnboardingField, transform: (OnboardingState) -> OnboardingState) = change {
        transform(it).copy(fieldErrors = it.fieldErrors - field, error = null)
    }

    private fun change(transform: (OnboardingState) -> OnboardingState) {
        _state.update(transform)
        persist(_state.value)
    }

    // ---- Validation and parsing ----

    private fun validate(step: OnboardingStep, s: OnboardingState): Set<OnboardingField> = buildSet {
        when (step) {
            OnboardingStep.Goal -> {
                if (s.goalName.isBlank()) add(OnboardingField.GOAL_NAME)
                if (MoneyParser.parseToMinor(s.goalTarget) == null) add(OnboardingField.GOAL_TARGET)
            }
            OnboardingStep.Income -> {
                if (optionalAmount(s.salaryBase) == null) add(OnboardingField.SALARY_BASE)
                if (s.salaryDay.toIntOrNull()?.let { it in 1..31 } != true) add(OnboardingField.SALARY_DAY)
                if (optionalAmount(s.ownEstimate) == null) add(OnboardingField.OWN_ESTIMATE)
            }
            OnboardingStep.Accounts -> {
                if (optionalAmount(s.bankBalance) == null) add(OnboardingField.BANK)
                if (optionalAmount(s.cashBalance) == null) add(OnboardingField.CASH)
                if (optionalAmount(s.goalFundBalance) == null) add(OnboardingField.GOAL_FUND)
            }
            OnboardingStep.Committed -> {
                if (optionalAmount(s.rent) == null) add(OnboardingField.RENT)
                if (optionalAmount(s.utilities) == null) add(OnboardingField.UTILITIES)
                if (optionalAmount(s.gym) == null) add(OnboardingField.GYM)
                if (optionalAmount(s.subscriptions) == null) add(OnboardingField.SUBSCRIPTIONS)
            }
            OnboardingStep.Preview -> Unit
        }
    }

    /**
     * An optional amount: blank means "not given" (0), a typed 0 is fine (an empty cash wallet is real),
     * anything else must pass MoneyParser (positive, at most 2 decimals, under the cap). Null = invalid.
     */
    private fun optionalAmount(text: String): Long? {
        if (text.isBlank()) return 0L
        val value = text.replace(",", "").trim().toBigDecimalOrNull() ?: return null
        if (value.signum() == 0) return 0L
        return MoneyParser.parseToMinor(text)
    }

    private fun toPlan(s: OnboardingState) = OnboardingPlan(
        goalName = s.goalName.trim(),
        goalTargetMinor = MoneyParser.parseToMinor(s.goalTarget) ?: 0L,
        goalMonths = s.goalMonths,
        salaryBaseMinor = optionalAmount(s.salaryBase) ?: 0L,
        salaryDay = s.salaryDay.toIntOrNull()?.coerceIn(1, 31) ?: 25,
        ownMonthlyEstimateMinor = optionalAmount(s.ownEstimate) ?: 0L,
        variableSources = s.variableSources,
        bankBalanceMinor = optionalAmount(s.bankBalance) ?: 0L,
        cashBalanceMinor = optionalAmount(s.cashBalance) ?: 0L,
        goalFundBalanceMinor = optionalAmount(s.goalFundBalance) ?: 0L,
        rentMinor = optionalAmount(s.rent) ?: 0L,
        utilitiesMinor = optionalAmount(s.utilities) ?: 0L,
        gymMinor = optionalAmount(s.gym) ?: 0L,
        subscriptionsMinor = optionalAmount(s.subscriptions) ?: 0L,
    )

    private fun previewFor(s: OnboardingState): DayZeroPreview {
        val plan = toPlan(s)
        return computeDayZeroPreview(
            DayZeroInput(
                targetMinor = plan.goalTargetMinor,
                savedMinor = plan.goalFundBalanceMinor, // BR-71: saved = goal fund balance
                months = plan.goalMonths,
                salaryMinor = plan.salaryBaseMinor,
                rentMinor = plan.rentMinor,
                ownEstimateMinor = plan.ownMonthlyEstimateMinor,
            ),
        )
    }

    // ---- Process death ----

    private fun persist(s: OnboardingState) {
        savedState[K_STEP] = s.step.ordinal
        savedState[K_GOAL_NAME] = s.goalName
        savedState[K_GOAL_TARGET] = s.goalTarget
        savedState[K_GOAL_MONTHS] = s.goalMonths
        savedState[K_SALARY] = s.salaryBase
        savedState[K_PAY_DAY] = s.salaryDay
        savedState[K_ESTIMATE] = s.ownEstimate
        savedState[K_SOURCES] = s.variableSources.joinToString(",") { it.name }
        savedState[K_BANK] = s.bankBalance
        savedState[K_CASH] = s.cashBalance
        savedState[K_FUND] = s.goalFundBalance
        savedState[K_RENT] = s.rent
        savedState[K_UTILITIES] = s.utilities
        savedState[K_GYM] = s.gym
        savedState[K_SUBS] = s.subscriptions
    }

    private fun restore(): OnboardingState {
        val step = OnboardingStep.entries.getOrElse(savedState[K_STEP] ?: 0) { OnboardingStep.Goal }
        val restored = OnboardingState(
            step = step,
            goalName = savedState[K_GOAL_NAME] ?: "",
            goalTarget = savedState[K_GOAL_TARGET] ?: "",
            goalMonths = savedState[K_GOAL_MONTHS] ?: OnboardingState.DEFAULT_MONTHS,
            salaryBase = savedState[K_SALARY] ?: "",
            salaryDay = savedState[K_PAY_DAY] ?: OnboardingState.DEFAULT_PAY_DAY,
            ownEstimate = savedState[K_ESTIMATE] ?: "",
            variableSources = (savedState.get<String>(K_SOURCES) ?: "")
                .split(",")
                .mapNotNull { name -> OnboardingIncomeKind.entries.firstOrNull { it.name == name } }
                .toSet(),
            bankBalance = savedState[K_BANK] ?: "",
            cashBalance = savedState[K_CASH] ?: "",
            goalFundBalance = savedState[K_FUND] ?: "",
            rent = savedState[K_RENT] ?: "",
            utilities = savedState[K_UTILITIES] ?: "",
            gym = savedState[K_GYM] ?: "",
            subscriptions = savedState[K_SUBS] ?: "",
        )
        return if (step == OnboardingStep.Preview) restored.copy(preview = previewFor(restored)) else restored
    }

    private companion object {
        const val K_STEP = "onb_step"
        const val K_GOAL_NAME = "onb_goal_name"
        const val K_GOAL_TARGET = "onb_goal_target"
        const val K_GOAL_MONTHS = "onb_goal_months"
        const val K_SALARY = "onb_salary"
        const val K_PAY_DAY = "onb_pay_day"
        const val K_ESTIMATE = "onb_estimate"
        const val K_SOURCES = "onb_sources"
        const val K_BANK = "onb_bank"
        const val K_CASH = "onb_cash"
        const val K_FUND = "onb_fund"
        const val K_RENT = "onb_rent"
        const val K_UTILITIES = "onb_utilities"
        const val K_GYM = "onb_gym"
        const val K_SUBS = "onb_subs"
    }
}
