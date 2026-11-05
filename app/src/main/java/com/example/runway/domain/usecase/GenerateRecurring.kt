package com.example.runway.domain.usecase

import com.example.runway.core.money.FxMath
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.MonthKey
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.RecurringRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * F-19 / BR-91: every active recurring item that is due becomes a transaction, then moves on.
 *
 * - Runs once when the session becomes Ready (see ShellViewModel).
 * - The transaction id is `{recurringId}_{yyyyMMdd}` and createdAt/updatedAt are the due date itself,
 *   so running it twice (or after a crash between the two writes) writes the SAME documents again.
 *   Nothing is duplicated, and the rules' "createdAt never changes" check still passes (AT-17).
 * - If the app was closed for months, every missed due date gets its own transaction, back-dated to that day.
 * - Dates move on from the item's ORIGINAL due date (31 Jan, 28 Feb, 31 Mar), so a short month never drags the day down.
 * - A foreign-currency item with no known rate is left alone for next time: we do not guess a rate.
 *
 * Returns how many transactions were written.
 */
class GenerateRecurring @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository,
    private val profileRepository: UserProfileRepository,
    private val time: TimeProvider,
) {

    suspend operator fun invoke(): AppResult<Int> {
        val zone = time.zone
        val endOfToday = time.today().plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)

        val due = when (val result = recurringRepository.getDue(endOfToday)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        if (due.isEmpty()) return AppResult.Success(0)

        val rates = if (due.any { it.currency != CurrencyCode.LKR }) {
            profileRepository.observeProfile().firstOrNull()?.lastFxRates.orEmpty()
        } else {
            emptyMap()
        }

        val transactions = mutableListOf<Transaction>()
        val advanced = mutableListOf<RecurringItem>()
        for (item in due) {
            val rate = if (item.currency == CurrencyCode.LKR) 1.0 else rates[item.currency.name]
            if (rate == null || rate <= 0.0) continue // wait for a rate

            val occurrences = occurrencesUpTo(item, endOfToday)
            occurrences.dates.forEach { transactions += toTransaction(item, it, rate) }
            advanced += item.copy(nextDueDate = occurrences.next)
        }
        if (transactions.isEmpty()) return AppResult.Success(0)

        // Transactions first: if we stop between the two writes, the next run rewrites the same ids.
        val written = transactionRepository.addAll(transactions)
        if (written is AppResult.Failure) return written

        for (item in advanced) {
            val updated = recurringRepository.update(item)
            if (updated is AppResult.Failure) return updated
        }
        return AppResult.Success(transactions.size)
    }

    private class Occurrences(val dates: List<Instant>, val next: Instant)

    private fun occurrencesUpTo(item: RecurringItem, until: Instant): Occurrences {
        val first = item.nextDueDate.atZone(time.zone)
        val monthsPerStep = if (item.frequency == RecurringFrequency.YEARLY) 12L else 1L

        val dates = mutableListOf<Instant>()
        var step = 0L
        var current = first
        while (!current.toInstant().isAfter(until) && dates.size < MAX_CATCH_UP) {
            dates += current.toInstant()
            step++
            current = first.plusMonths(step * monthsPerStep) // from the original date, never from the previous result
        }
        return Occurrences(dates, current.toInstant())
    }

    private fun toTransaction(item: RecurringItem, due: Instant, rate: Double): Transaction {
        val day = due.atZone(time.zone).toLocalDate()
        return Transaction(
            id = "${item.id}_${day.format(DateTimeFormatter.BASIC_ISO_DATE)}",
            type = TransactionType.EXPENSE,
            amountMinor = item.amountMinor,
            currency = item.currency,
            fxRate = rate,
            amountLkrMinor = FxMath.toLkrMinor(item.amountMinor, rate),
            categoryId = item.categoryId,
            accountId = item.accountId,
            toAccountId = null,
            incomeSourceId = null,
            relatedTxId = null,
            note = item.name,
            date = due,
            monthKey = MonthKey.of(due, time.zone),
            archived = false,
            createdAt = due,
            updatedAt = due,
        )
    }

    private companion object {
        /** A guard against a runaway loop: a year of missed months at most per item per run. */
        const val MAX_CATCH_UP = 12
    }
}
