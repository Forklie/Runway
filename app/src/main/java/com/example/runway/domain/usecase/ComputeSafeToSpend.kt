package com.example.runway.domain.usecase

import com.example.runway.core.money.FxMath
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.LocalDate
import javax.inject.Inject

data class SafeToSpendInput(
    val reliableIncomeExpected: Long,
    val variableIncomeReceived: Long,
    val committedCosts: Long,
    val goalContribution: Long,
    val discretionarySpent: Long,
    val today: LocalDate,
)

data class SafeToSpend(val monthBudget: Long, val remaining: Long, val perDay: Long, val daysLeft: Int)

/** The yellow pill on Home. Copy stays neutral: never "overspent" (Doc 1 section 3.1). */
enum class SpendPace { ON_PACE, FASTER, OVER }

/**
 * Safe to Spend, refined definition (Doc 1 section 8.9). It plans on reliable money only: expected
 * variable income never inflates it (BR-43). All inputs are LKR minor units.
 */
class ComputeSafeToSpend @Inject constructor() {

    operator fun invoke(i: SafeToSpendInput): SafeToSpend {
        val daysLeft = i.today.lengthOfMonth() - i.today.dayOfMonth + 1 // including today
        val budget = i.reliableIncomeExpected + i.variableIncomeReceived - i.committedCosts - i.goalContribution
        val remaining = budget - i.discretionarySpent
        return SafeToSpend(budget, remaining, Math.floorDiv(remaining, daysLeft.toLong()), daysLeft)
    }

    /** Over plan, or spending faster than an even share of the budget for the days gone so far. */
    fun pace(result: SafeToSpend, discretionarySpent: Long, today: LocalDate): SpendPace {
        if (result.remaining < 0) return SpendPace.OVER
        val fairShareSoFar = Math.floorDiv(result.monthBudget * today.dayOfMonth, today.lengthOfMonth().toLong())
        return if (discretionarySpent > fairShareSoFar) SpendPace.FASTER else SpendPace.ON_PACE
    }

    /** Salary base and any other RELIABLE source, counted before it arrives (BR-40). */
    fun reliableIncomeExpected(sources: List<IncomeSource>, fxRates: Map<String, Double>): Long =
        sources.filter { it.reliability == Reliability.RELIABLE }
            .sumOf { toLkr(it.expectedAmountMinor, it.currency, fxRates) ?: 0L }

    /**
     * Variable income already received this month, plus salary received above its base (the bonus,
     * BR-40). Crypto cash-outs are left out: crypto never affects Safe to Spend (BR-62).
     */
    fun variableIncomeReceived(
        sources: List<IncomeSource>,
        monthTransactions: List<Transaction>,
        fxRates: Map<String, Double>,
    ): Long {
        val byId = sources.associateBy { it.id }
        val income = monthTransactions.filter { it.type == TransactionType.INCOME && !it.archived }
        val (reliable, other) = income.partition { byId[it.incomeSourceId]?.reliability == Reliability.RELIABLE }

        val aboveBase = reliable.groupBy { it.incomeSourceId }.entries.sumOf { (sourceId, received) ->
            val source = byId[sourceId] ?: return@sumOf 0L
            val base = toLkr(source.expectedAmountMinor, source.currency, fxRates) ?: 0L
            (received.sumOf { it.amountLkrMinor } - base).coerceAtLeast(0L)
        }
        val variable = other.filterNot { byId[it.incomeSourceId]?.kind == IncomeSourceKind.CRYPTO }
            .sumOf { it.amountLkrMinor }
        return aboveBase + variable
    }

    /** Planned committed costs for one month from active recurring items. Yearly items count 1/12. */
    fun plannedCommitted(recurring: List<RecurringItem>, fxRates: Map<String, Double>): Long =
        recurring.filter { it.active }.sumOf { item ->
            val monthly = when (item.frequency) {
                RecurringFrequency.MONTHLY -> item.amountMinor
                RecurringFrequency.YEARLY -> (item.amountMinor + 11) / 12
            }
            toLkr(monthly, item.currency, fxRates) ?: 0L
        }

    /** Committed costs: the plan, or what was actually spent if that is higher (Doc 1 section 8.9). */
    fun committedCosts(plannedMinor: Long, actualCommittedMinor: Long): Long = maxOf(plannedMinor, actualCommittedMinor)

    /** A foreign amount without a remembered rate is left out rather than guessed. */
    private fun toLkr(amountMinor: Long, currency: CurrencyCode, fxRates: Map<String, Double>): Long? =
        if (currency == CurrencyCode.LKR) {
            amountMinor
        } else {
            fxRates[currency.name]?.takeIf { it > 0.0 }?.let { FxMath.toLkrMinor(amountMinor, it) }
        }
}
