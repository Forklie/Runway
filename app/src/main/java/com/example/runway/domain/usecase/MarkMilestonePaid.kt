package com.example.runway.domain.usecase

import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.DefaultIncomeSourceIds
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.MilestoneStatus
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.FreelanceRepository
import javax.inject.Inject

/**
 * BR-52: PAID creates the INCOME transaction and links it, so a payment always says which project it was for.
 * The transaction's id comes from the milestone's, so a double tap or retry rewrites one document (BR-19).
 * ponytail: freelance income is LKR here. A USD client goes through Log income; add a rate field if that becomes common.
 */
class MarkMilestonePaid @Inject constructor(
    private val addTransaction: AddTransaction,
    private val repository: FreelanceRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(milestone: Milestone, projectTitle: String, amountMinor: Long, accountId: String?): AppResult<Unit> {
        val draft = TransactionDraft(
            id = "milestone-${milestone.id}",
            type = TransactionType.INCOME,
            amountMinor = amountMinor,
            date = time.now(),
            accountId = accountId,
            incomeSourceId = DefaultIncomeSourceIds.FREELANCE,
            note = "$projectTitle: ${milestone.label}".take(AddTransaction.MAX_NOTE),
        )
        val income = when (val built = addTransaction.build(draft)) {
            is AppResult.Failure -> return built
            is AppResult.Success -> built.value
        }
        val paid = milestone.copy(status = MilestoneStatus.PAID, paidDate = income.date, linkedTxId = income.id)
        return repository.markPaid(paid, income)
    }
}
