package com.example.runway.ui.feature.onboarding

import com.example.runway.core.result.AppError
import com.example.runway.domain.model.OnboardingIncomeKind
import com.example.runway.domain.usecase.DayZeroPreview

/** The five onboarding screens (Doc 1 §9.1). Only the goal is required, so only the middle three can be skipped. */
enum class OnboardingStep(val number: Int, val skippable: Boolean) {
    Goal(1, skippable = false),
    Income(2, skippable = true),
    Accounts(3, skippable = true),
    Committed(4, skippable = true),
    Preview(5, skippable = false),
    ;

    companion object {
        const val COUNT = 5
    }

    fun next(): OnboardingStep = entries.getOrElse(ordinal + 1) { this }
    fun previous(): OnboardingStep = entries.getOrElse(ordinal - 1) { this }
}

/** Every input that can show an inline error. The screen maps each one to a message. */
enum class OnboardingField {
    GOAL_NAME, GOAL_TARGET,
    SALARY_BASE, SALARY_DAY, OWN_ESTIMATE,
    BANK, CASH, GOAL_FUND,
    RENT, UTILITIES, GYM, SUBSCRIPTIONS,
}

/**
 * Everything the five screens show. Amounts stay as the String the user typed (never a Double) and are
 * parsed with MoneyParser on Next. The text is kept on every error.
 */
data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Goal,

    // 1 Goal (required)
    val goalName: String = "",
    val goalTarget: String = "",
    val goalMonths: Int = DEFAULT_MONTHS,

    // 2 Income (skippable)
    val salaryBase: String = "",
    val salaryDay: String = DEFAULT_PAY_DAY,
    val ownEstimate: String = "",
    val variableSources: Set<OnboardingIncomeKind> = emptySet(),

    // 3 Accounts and opening balances (skippable)
    val bankBalance: String = "",
    val cashBalance: String = "",
    val goalFundBalance: String = "",

    // 4 Committed costs per month (skippable)
    val rent: String = "",
    val utilities: String = "",
    val gym: String = "",
    val subscriptions: String = "",

    // 5 Preview, computed when the step is reached
    val preview: DayZeroPreview? = null,

    val fieldErrors: Set<OnboardingField> = emptySet(),
    /** The batch could not be queued (for example no signed-in user). The input is kept. */
    val error: AppError? = null,
    val submitting: Boolean = false,
) {
    companion object {
        const val DEFAULT_MONTHS = 12
        const val DEFAULT_PAY_DAY = "25"
        val MONTH_CHOICES = listOf(6, 12, 18, 24)
    }
}
