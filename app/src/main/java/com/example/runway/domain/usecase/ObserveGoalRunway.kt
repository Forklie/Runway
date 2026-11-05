package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Goal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GoalRunway(
    val goal: Goal,
    val projection: GoalProjection,
    val fundName: String,
    /** Where "Add to fund" can move money from: every other account, with its derived balance. */
    val fromAccounts: List<AccountBalance>,
)

/** The Goal screen's data (FR-14): the goal, its live projection and the accounts that can feed it. */
class ObserveGoalRunway @Inject constructor(
    private val observeLedger: ObserveLedger,
    private val balances: ComputeAccountBalances,
    private val projectGoal: ProjectGoal,
) {
    /** Null until a goal exists: the screen shows "Set your goal". */
    operator fun invoke(): Flow<GoalRunway?> = observeLedger().map(::summarise)

    fun summarise(ledger: Ledger): GoalRunway? {
        val goal = ledger.goal ?: return null
        val all = balances(ledger.accounts, ledger.transactions)
        val fund = all.firstOrNull { it.account.id == goal.fundAccountId }
        return GoalRunway(
            goal = goal,
            projection = projectGoal(goal, fund?.balanceMinor ?: 0L, ledger.transactions, ledger.today, ledger.zone),
            fundName = fund?.account?.name ?: goal.name,
            fromAccounts = all.filter { it.account.id != goal.fundAccountId && it.account.kind != AccountKind.GOAL_FUND },
        )
    }
}
