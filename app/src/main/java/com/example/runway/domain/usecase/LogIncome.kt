package com.example.runway.domain.usecase

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Income, plus the Pay Yourself First transfer when [pyfPercent] and [goalFundId] are given (F-09, FR-12).
 * Both go in one batch, so neither can land without the other (AT-08). The transfer's id comes from the
 * income's, so a retry rewrites the same two documents (BR-19). The rate used becomes the next default (BR-06).
 */
class LogIncome @Inject constructor(
    private val addTransaction: AddTransaction,
    private val transactions: TransactionRepository,
    private val profile: UserProfileRepository,
) {
    suspend operator fun invoke(
        income: TransactionDraft,
        pyfPercent: Double? = null,
        goalFundId: String? = null,
    ): AppResult<Transaction> {
        val built = when (val result = addTransaction.build(income)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.value
        }
        val transferMinor = pyfPercent?.let { pyfMinor(built.amountLkrMinor, it) } ?: 0L
        val transfer = if (goalFundId != null && transferMinor > 0L) {
            val draft = TransactionDraft(
                id = "${income.id}-pyf",
                type = TransactionType.TRANSFER,
                amountMinor = transferMinor,
                date = income.date,
                accountId = income.accountId,
                toAccountId = goalFundId,
            )
            when (val result = addTransaction.build(draft)) {
                is AppResult.Failure -> return result
                is AppResult.Success -> result.value
            }
        } else {
            null
        }
        return when (val written = transactions.addAll(listOfNotNull(built, transfer))) {
            is AppResult.Failure -> written
            is AppResult.Success -> {
                if (built.currency != CurrencyCode.LKR) profile.setLastFxRate(built.currency, built.fxRate)
                AppResult.Success(built)
            }
        }
    }

    companion object {
        /** The goal fund's share of an income. Rounded like every LKR value (BR-03). */
        fun pyfMinor(incomeLkrMinor: Long, percent: Double): Long =
            BigDecimal(incomeLkrMinor).multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal(100), 0, RoundingMode.HALF_UP).longValueExact()
    }
}
