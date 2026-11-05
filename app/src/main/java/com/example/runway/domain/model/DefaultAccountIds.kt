package com.example.runway.domain.model

/**
 * Deterministic ids of the accounts created at sign-up. Onboarding (step 3) writes opening balances
 * to these same ids, so nothing is duplicated. Exactly one account is the goal fund (BR-30).
 */
object DefaultAccountIds {
    const val BANK_MAIN = "bank-main"
    const val CASH = "cash"
    const val GOAL_FUND = "goal-fund"
}
