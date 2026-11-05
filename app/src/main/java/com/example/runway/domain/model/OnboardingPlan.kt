package com.example.runway.domain.model

/** The variable income sources a user can tick in onboarding step 2 (BR-41: counted only once received). */
enum class OnboardingIncomeKind { FREELANCE, ADSENSE, CRYPTO, OTHER }

/**
 * Everything onboarding collected, in Long minor units, ready to be written in one batch (FR-05).
 * 0 means "not given" for every optional amount, so skipping a step is just leaving zeros.
 * Only the goal (name, target, months) is required.
 */
data class OnboardingPlan(
    val goalName: String,
    val goalTargetMinor: Long,
    val goalMonths: Int,
    val salaryBaseMinor: Long = 0L,
    val salaryDay: Int = 25,
    val ownMonthlyEstimateMinor: Long = 0L,
    val variableSources: Set<OnboardingIncomeKind> = emptySet(),
    val bankBalanceMinor: Long = 0L,
    val cashBalanceMinor: Long = 0L,
    val goalFundBalanceMinor: Long = 0L,
    val rentMinor: Long = 0L,
    val utilitiesMinor: Long = 0L,
    val gymMinor: Long = 0L,
    val subscriptionsMinor: Long = 0L,
)
