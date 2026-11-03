package com.example.runway.domain.usecase

import com.example.runway.domain.model.Account
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.Instant
import javax.inject.Inject

data class AccountBalance(val account: Account, val balanceMinor: Long)

/**
 * BR-31 and BR-80: balance = last checkpoint + money in − money out since that checkpoint.
 * Nothing is stored, so nothing drifts. Only movements AFTER checkpointDate count, because the
 * checkpoint (opening balance or a check-in) already includes everything before it.
 */
class ComputeAccountBalances @Inject constructor() {

    operator fun invoke(accounts: List<Account>, transactions: List<Transaction>): List<AccountBalance> =
        accounts.map { AccountBalance(it, balanceOf(it, transactions)) }

    fun balanceOf(account: Account, transactions: List<Transaction>): Long =
        account.checkpointBalanceMinor + transactions
            .filter { !it.archived && it.date.isAfter(account.checkpointDate) }
            .sumOf { effectOn(account.id, it) }

    /** Net movement of one account in [from, to). Goal pace uses this on the goal fund (BR-73). */
    fun netChange(accountId: String, transactions: List<Transaction>, from: Instant, to: Instant): Long =
        transactions
            .filter { !it.archived && !it.date.isBefore(from) && it.date.isBefore(to) }
            .sumOf { effectOn(accountId, it) }

    /** Signed LKR effect of one transaction on one account (BR-11 to BR-14). */
    fun effectOn(accountId: String, tx: Transaction): Long = when (tx.type) {
        TransactionType.EXPENSE -> if (tx.accountId == accountId) -tx.amountLkrMinor else 0L
        TransactionType.INCOME,
        TransactionType.REFUND -> if (tx.accountId == accountId) tx.amountLkrMinor else 0L
        // A transfer only moves money: out of one account, into the other, never income or expense.
        TransactionType.TRANSFER -> when (accountId) {
            tx.accountId -> -tx.amountLkrMinor
            tx.toAccountId -> tx.amountLkrMinor
            else -> 0L
        }
    }
}
