package com.example.runway.domain.model

/**
 * Fixed document ids written by onboarding (same idea as DefaultAccountIds). Fixed ids mean running
 * onboarding twice overwrites the same documents instead of duplicating them.
 * A: the income source ids below are what Log income should look up.
 */
object DefaultGoalIds {
    const val MAIN = "main" // users/{uid}/goals/main (BR-70: one active goal)
}

object DefaultIncomeSourceIds {
    const val SALARY = "salary"
    const val FREELANCE = "freelance"
    const val ADSENSE = "adsense"
    const val CRYPTO = "crypto"
    const val OTHER = "other-income"
}

object DefaultRecurringIds {
    const val RENT = "recurring-rent"
    const val UTILITIES = "recurring-utilities"
    const val GYM = "recurring-gym"
    const val SUBSCRIPTIONS = "recurring-subscriptions"
}
