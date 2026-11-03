package com.example.runway.domain.usecase

import com.example.runway.core.money.MoneyParser
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.GapResolution
import com.example.runway.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One account in a check-in, with the balance the app expects (BR-80). */
data class CheckinAccount(val account: Account, val expectedMinor: Long)

data class CheckinSetup(
    val accounts: List<CheckinAccount>,
    val categories: List<Category>,
    val lastCheckin: Instant?,
)

/** What the user typed for one account. */
data class EnteredBalance(val account: CheckinAccount, val enteredMinor: Long)

/** Everything one check-in writes: the records (and new checkpoints) and any gaps logged as spending. */
data class CheckinPlan(val checkins: List<BalanceCheckin>, val gapExpenses: List<TransactionDraft>)

/** Live expected balances for the check-in flow. */
class ObserveCheckinSetup @Inject constructor(
    private val observeLedger: ObserveLedger,
    private val balances: ComputeAccountBalances,
) {
    operator fun invoke(): Flow<CheckinSetup> = observeLedger().map(::summarise)

    fun summarise(ledger: Ledger) = CheckinSetup(
        accounts = ledger.accounts.map { CheckinAccount(it, balances.balanceOf(it, ledger.transactions)) },
        categories = ledger.categories,
        lastCheckin = ledger.checkins.maxOfOrNull { it.date },
    )
}

/**
 * The weekly check-in rules (Doc 1 section 8.10). Pure: the ViewModel passes what was typed and gets back
 * exactly what to write. Ids come from one session id, so saving twice rewrites the same documents.
 */
class PlanCheckin @Inject constructor(
    private val unaccounted: ComputeUnaccounted,
) {
    /**
     * @param logGapsAs category for "log it as spending" (BR-84), or null to leave gaps as Unaccounted.
     * @param at the check-in time; it becomes every account's new checkpoint date (BR-83).
     */
    fun plan(
        sessionId: String,
        entered: List<EnteredBalance>,
        logGapsAs: String?,
        gapNote: String,
        at: Instant,
    ): CheckinPlan {
        val checkins = mutableListOf<BalanceCheckin>()
        val expenses = mutableListOf<TransactionDraft>()
        entered.forEach { (item, enteredMinor) ->
            val gap = unaccounted.gap(item.expectedMinor, enteredMinor)
            val logIt = logGapsAs != null && gap > 0
            checkins += BalanceCheckin(
                id = "${sessionId}_${item.account.id}",
                date = at,
                accountId = item.account.id,
                enteredBalanceMinor = enteredMinor,
                expectedBalanceMinor = item.expectedMinor,
                gapMinor = gap,
                resolution = if (logIt) GapResolution.LOGGED else unaccounted.defaultResolution(gap),
            )
            if (logIt) {
                // Dated just before the checkpoint: the new checkpoint already includes this money.
                expenses += TransactionDraft(
                    id = "${sessionId}_${item.account.id}_gap",
                    type = TransactionType.EXPENSE,
                    amountMinor = gap,
                    date = at.minusMillis(1),
                    categoryId = logGapsAs,
                    accountId = item.account.id,
                    note = gapNote,
                )
            }
        }
        return CheckinPlan(checkins, expenses)
    }

    /** Like MoneyParser, but a balance may be zero (an empty wallet). Null when it is not a valid amount. */
    fun parseBalance(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        val value = cleaned.toBigDecimalOrNull() ?: return null
        return if (value.compareTo(BigDecimal.ZERO) == 0) 0L else MoneyParser.parseToMinor(cleaned)
    }
}
