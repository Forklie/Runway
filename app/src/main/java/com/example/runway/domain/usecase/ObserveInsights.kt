package com.example.runway.domain.usecase

import com.example.runway.core.time.MonthKey
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.repository.CryptoRepository
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class Insights(
    val today: LocalDate,
    val incomeReality: IncomeReality,
    val balances: List<AccountBalance>,
    val lastCheckin: Instant?,
    val daysSinceCheckin: Int?,
    val clarityPercent: Int?,
    val crypto: CryptoPosition,
    val hasCryptoEntries: Boolean,
)

/**
 * The Insights tab: income reality (F-12), derived balances with the last check-in (F-13) and the
 * crypto net position (F-17). [summarise] is pure and unit tested.
 */
class ObserveInsights @Inject constructor(
    private val observeLedger: ObserveLedger,
    private val cryptoRepository: CryptoRepository,
    private val balances: ComputeAccountBalances,
    private val incomeReality: ComputeIncomeReality,
    private val monthSpending: ComputeMonthSpending,
    private val unaccounted: ComputeUnaccounted,
    private val cryptoPosition: ComputeCryptoPosition,
) {
    operator fun invoke(): Flow<Insights> =
        combine(observeLedger(), cryptoRepository.observeEntries(), ::summarise)

    fun summarise(ledger: Ledger, cryptoEntries: List<CryptoEntry>): Insights {
        val monthKey = MonthKey.of(ledger.today)
        val lastCheckin = ledger.checkins.maxOfOrNull { it.date }
        val monthCheckins = ledger.checkins.filter { MonthKey.of(it.date, ledger.zone) == monthKey }
        val clarity = if (monthCheckins.isEmpty()) {
            null
        } else {
            val logged = monthSpending(ledger.transactions.filter { it.monthKey == monthKey }, ledger.categories).loggedExpensesMinor
            unaccounted.clarityPercent(logged, unaccounted.unaccountedSpending(monthCheckins))
        }
        return Insights(
            today = ledger.today,
            incomeReality = incomeReality(ledger.sources, ledger.transactions, ledger.today, ledger.zone),
            balances = balances(ledger.accounts, ledger.transactions),
            lastCheckin = lastCheckin,
            daysSinceCheckin = lastCheckin?.let {
                ChronoUnit.DAYS.between(it.atZone(ledger.zone).toLocalDate(), ledger.today).toInt()
            },
            clarityPercent = clarity,
            crypto = cryptoPosition(cryptoEntries),
            hasCryptoEntries = cryptoEntries.isNotEmpty(),
        )
    }
}
