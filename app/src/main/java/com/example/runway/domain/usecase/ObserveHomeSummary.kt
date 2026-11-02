package com.example.runway.domain.usecase

import com.example.runway.core.time.MonthKey
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A gentle Home prompt (design system Note card). At most one at a time. */
sealed interface HomePrompt {
    /** BR-85: no entry or check-in for 3 or more days. Balance check first, no scolding. */
    data class WelcomeBack(val daysAway: Int) : HomePrompt

    /** Weekly check-in is due. [daysSince] is null when he has never checked in. */
    data class CheckinDue(val daysSince: Int?) : HomePrompt
}

data class HomeSummary(
    val firstName: String,
    val today: LocalDate,
    val safeToSpend: SafeToSpend,
    val safeToSpendInput: SafeToSpendInput, // shown in "How we worked this out"
    val pace: SpendPace,
    val goal: Goal?,
    val projection: GoalProjection?,
    val month: MonthSpending,
    val clarityPercent: Int?,
    val prompt: HomePrompt?,
    val todayActivity: List<Transaction>,
    val accountsById: Map<String, Account>,
    val categoriesById: Map<String, Category>,
    val sourcesById: Map<String, IncomeSource>,
    /** Nothing at all yet (the sign-up seed has not arrived): Home shows its empty state (AT-01). */
    val isEmpty: Boolean,
)

/**
 * Home's numbers (FR-13): Safe to Spend, goal runway, committed vs discretionary, top categories,
 * Clarity and the prompt, all from live listeners. [summarise] is pure, so every rule is unit tested.
 */
class ObserveHomeSummary @Inject constructor(
    private val observeLedger: ObserveLedger,
    private val balances: ComputeAccountBalances,
    private val safeToSpend: ComputeSafeToSpend,
    private val projectGoal: ProjectGoal,
    private val monthSpending: ComputeMonthSpending,
    private val unaccounted: ComputeUnaccounted,
) {
    operator fun invoke(): Flow<HomeSummary> = observeLedger().map(::summarise)

    fun summarise(ledger: Ledger): HomeSummary {
        val monthKey = MonthKey.of(ledger.today)
        val monthTx = ledger.transactions.filter { it.monthKey == monthKey }
        val month = monthSpending(monthTx, ledger.categories)
        val fxRates = ledger.profile?.lastFxRates.orEmpty()

        val projection = ledger.goal?.let { goal ->
            val fund = ledger.accounts.firstOrNull { it.id == goal.fundAccountId }
            val saved = fund?.let { balances.balanceOf(it, ledger.transactions) } ?: 0L
            projectGoal(goal, saved, ledger.transactions, ledger.today, ledger.zone)
        }

        val input = SafeToSpendInput(
            reliableIncomeExpected = safeToSpend.reliableIncomeExpected(ledger.sources, fxRates),
            variableIncomeReceived = safeToSpend.variableIncomeReceived(ledger.sources, monthTx, fxRates),
            committedCosts = safeToSpend.committedCosts(
                plannedMinor = safeToSpend.plannedCommitted(ledger.recurring, fxRates),
                actualCommittedMinor = month.committedMinor,
            ),
            goalContribution = projection?.requiredPerMonthMinor ?: 0L,
            discretionarySpent = month.discretionaryMinor,
            today = ledger.today,
        )
        val result = safeToSpend(input)

        val monthCheckins = ledger.checkins.filter { MonthKey.of(it.date, ledger.zone) == monthKey }
        val clarity = if (monthCheckins.isEmpty()) {
            null // Clarity needs at least one balance check this month (BR-82)
        } else {
            unaccounted.clarityPercent(month.loggedExpensesMinor, unaccounted.unaccountedSpending(monthCheckins))
        }

        return HomeSummary(
            firstName = ledger.profile?.displayName?.trim()?.substringBefore(' ').orEmpty(),
            today = ledger.today,
            safeToSpend = result,
            safeToSpendInput = input,
            pace = safeToSpend.pace(result, month.discretionaryMinor, ledger.today),
            goal = ledger.goal,
            projection = projection,
            month = month,
            clarityPercent = clarity,
            prompt = prompt(ledger),
            todayActivity = monthTx
                .filter { it.date.atZone(ledger.zone).toLocalDate() == ledger.today }
                .sortedByDescending { it.date }
                .take(TODAY_ROWS),
            accountsById = ledger.accounts.associateBy { it.id },
            categoriesById = ledger.categories.associateBy { it.id },
            sourcesById = ledger.sources.associateBy { it.id },
            isEmpty = ledger.accounts.isEmpty() && ledger.goal == null &&
                ledger.sources.isEmpty() && ledger.transactions.isEmpty(),
        )
    }

    /**
     * BR-85 first: when the last entry or check-in is 3+ days old, welcome him back. Otherwise, once a
     * week (and never in his first week), suggest the check-in.
     */
    private fun prompt(ledger: Ledger): HomePrompt? {
        val firstSeen = ledger.profile?.createdAt ?: ledger.accounts.minOfOrNull { it.createdAt } ?: return null
        val lastCheckin = ledger.checkins.maxOfOrNull { it.date }
        val lastEntry = ledger.transactions.maxOfOrNull { it.createdAt }
        val lastActivity = listOfNotNull(firstSeen, lastCheckin, lastEntry).max()

        fun daysSince(instant: Instant): Int =
            ChronoUnit.DAYS.between(instant.atZone(ledger.zone).toLocalDate(), ledger.today).toInt()

        val away = daysSince(lastActivity)
        if (away >= WELCOME_BACK_DAYS) return HomePrompt.WelcomeBack(away)

        val sinceCheckin = lastCheckin?.let(::daysSince)
        val checkinDue = if (sinceCheckin == null) daysSince(firstSeen) >= CHECKIN_DAYS else sinceCheckin >= CHECKIN_DAYS
        return if (checkinDue) HomePrompt.CheckinDue(sinceCheckin) else null
    }

    private companion object {
        const val WELCOME_BACK_DAYS = 3
        const val CHECKIN_DAYS = 7
        const val TODAY_ROWS = 8
    }
}

/** Expenses minus refunds logged today, for the "Today" section total. */
fun List<Transaction>.spentMinor(): Long =
    filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountLkrMinor } -
        filter { it.type == TransactionType.REFUND }.sumOf { it.amountLkrMinor }
