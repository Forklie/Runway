package com.example.runway.domain.usecase

import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.BalanceCheckinRepository
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.GoalRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import com.example.runway.domain.repository.RecurringRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Everything the Home, Goal, Insights and Check-in formulas read, as one consistent snapshot. */
data class Ledger(
    val now: Instant,
    val today: LocalDate,
    val zone: ZoneId,
    val profile: UserProfile?,
    val accounts: List<Account>,
    val goal: Goal?,
    val sources: List<IncomeSource>,
    val categories: List<Category>,
    val transactions: List<Transaction>, // non-archived, newest first, since windowStart
    val checkins: List<BalanceCheckin>,  // newest first
    val recurring: List<RecurringItem>,
)

/**
 * Live listeners on every collection slice C needs, combined into one [Ledger]. Each repository flow
 * re-emits on every change (local writes included), so the screens update with no refresh button.
 * The transaction listener only restarts when its window start moves (after a check-in).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveLedger @Inject constructor(
    private val profiles: UserProfileRepository,
    private val accounts: AccountRepository,
    private val goals: GoalRepository,
    private val sources: IncomeSourceRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val checkins: BalanceCheckinRepository,
    private val recurring: Optional<RecurringRepository>,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<Ledger> {
        val today = time.today()
        val accountList = accounts.observeAccounts()
        val movements = accountList
            .map { windowStart(it, today, time.zone) }
            .distinctUntilChanged()
            .flatMapLatest { transactions.observeSince(it) }
        val planned = if (recurring.isPresent) recurring.get().observeAll() else flowOf(emptyList())

        val setup = combine(
            profiles.observeProfile(),
            goals.observeActiveGoal(),
            sources.observeSources(),
            categories.observeCategories(),
            checkins.observeAll(),
        ) { profile, goal, sourceList, categoryList, checkinList ->
            Setup(profile, goal, sourceList, categoryList, checkinList)
        }

        return combine(setup, accountList, movements, planned) { s, accs, txs, items ->
            Ledger(
                now = time.now(),
                today = today,
                zone = time.zone,
                profile = s.profile,
                accounts = accs,
                goal = s.goal,
                sources = s.sources,
                categories = s.categories,
                transactions = txs,
                checkins = s.checkins,
                recurring = items,
            )
        }
    }

    private data class Setup(
        val profile: UserProfile?,
        val goal: Goal?,
        val sources: List<IncomeSource>,
        val categories: List<Category>,
        val checkins: List<BalanceCheckin>,
    )

    companion object {
        /**
         * The oldest moment any slice C formula needs: every account's checkpoint (balances, BR-31),
         * 90 days of goal pace (BR-73) and the first of the 3 complete months of income (BR-44).
         */
        fun windowStart(accounts: List<Account>, today: LocalDate, zone: ZoneId): Instant {
            val incomeWindow = today.withDayOfMonth(1).minusMonths(3).atStartOfDay(zone).toInstant()
            val paceWindow = today.minusDays(90).atStartOfDay(zone).toInstant()
            val oldestCheckpoint = accounts.minOfOrNull { it.checkpointDate } ?: incomeWindow
            return minOf(incomeWindow, paceWindow, oldestCheckpoint)
        }
    }
}
