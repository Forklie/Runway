package com.example.runway.data.repository

import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toTimestamp
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.DefaultAccountIds
import com.example.runway.domain.model.DefaultGoalIds
import com.example.runway.domain.model.DefaultIncomeSourceIds
import com.example.runway.domain.model.DefaultRecurringIds
import com.example.runway.domain.model.OnboardingIncomeKind
import com.example.runway.domain.model.OnboardingPlan
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.RecurringKind
import com.example.runway.domain.repository.OnboardingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onboarding's single batch (FR-05). Ids are fixed and nothing is awaited, exactly like DefaultSeed:
 * the local cache updates at once, the batch syncs when online, and a repeat run overwrites itself.
 * Documents that already exist (users/{uid}, the three accounts) are MERGED, so nothing else on them is lost.
 * Income sources and the goal use plain maps with the Doc 2 §6.2 field names, so this does not
 * depend on A's IncomeSource or C's Goal models.
 */
@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val clock: Clock,
) : OnboardingRepository {

    override suspend fun complete(plan: OnboardingPlan): AppResult<Unit> = appResultOf {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        val now = clock.instant()
        val userRef = firestore.collection("users").document(uid)
        val batch = firestore.batch()

        batch.set(
            userRef,
            mapOf("salaryDay" to plan.salaryDay, "onboardingComplete" to true),
            SetOptions.merge(),
        )
        writeGoal(batch, userRef, plan, now)
        writeIncomeSources(batch, userRef, plan)
        writeOpeningBalances(batch, userRef, plan, now)
        writeCommittedCosts(batch, userRef, plan, now)

        batch.commit().logWriteFailure("onboarding batch users/$uid")
    }

    /** goals/main. The goal fund account is the one whose balance counts as saved (BR-71). */
    private fun writeGoal(batch: WriteBatch, userRef: DocumentReference, plan: OnboardingPlan, now: Instant) {
        val deadline = now.atZone(clock.zone).plusMonths(plan.goalMonths.toLong()).toInstant()
        batch.set(
            userRef.collection("goals").document(DefaultGoalIds.MAIN),
            mapOf(
                "name" to plan.goalName,
                "targetMinor" to plan.goalTargetMinor,
                "startDate" to now.toTimestamp(),
                "deadline" to deadline.toTimestamp(),
                "fundAccountId" to DefaultAccountIds.GOAL_FUND,
                "active" to true,
            ),
        )
    }

    /** Salary is RELIABLE (BR-40). The ticked extras are VARIABLE and count only once received (BR-41). */
    private fun writeIncomeSources(batch: WriteBatch, userRef: DocumentReference, plan: OnboardingPlan) {
        val sources = userRef.collection("incomeSources")

        if (plan.salaryBaseMinor > 0L || plan.ownMonthlyEstimateMinor > 0L) {
            batch.set(
                sources.document(DefaultIncomeSourceIds.SALARY),
                mapOf(
                    "name" to "Salary",
                    "kind" to "SALARY",
                    "currency" to CurrencyCode.LKR.name,
                    "reliability" to "RELIABLE",
                    "expectedAmountMinor" to plan.salaryBaseMinor,
                    "payDay" to plan.salaryDay,
                    "ownMonthlyEstimateMinor" to plan.ownMonthlyEstimateMinor,
                ),
            )
        }

        plan.variableSources.forEach { kind ->
            val (id, name, currency) = when (kind) {
                OnboardingIncomeKind.FREELANCE -> Triple(DefaultIncomeSourceIds.FREELANCE, "Freelance", CurrencyCode.LKR)
                OnboardingIncomeKind.ADSENSE -> Triple(DefaultIncomeSourceIds.ADSENSE, "AdSense", CurrencyCode.USD)
                OnboardingIncomeKind.CRYPTO -> Triple(DefaultIncomeSourceIds.CRYPTO, "Crypto", CurrencyCode.LKR)
                OnboardingIncomeKind.OTHER -> Triple(DefaultIncomeSourceIds.OTHER, "Other income", CurrencyCode.LKR)
            }
            batch.set(
                sources.document(id),
                mapOf(
                    "name" to name,
                    "kind" to kind.name,
                    "currency" to currency.name,
                    "reliability" to "VARIABLE",
                    "expectedAmountMinor" to 0L,
                ),
            )
        }
    }

    /** BR-32: the opening balance is the first checkpoint. Merge keeps createdAt from the sign-up seed. */
    private fun writeOpeningBalances(batch: WriteBatch, userRef: DocumentReference, plan: OnboardingPlan, now: Instant) {
        val accounts = userRef.collection("accounts")
        listOf(
            Triple(DefaultAccountIds.BANK_MAIN, "Main bank" to AccountKind.BANK, plan.bankBalanceMinor),
            Triple(DefaultAccountIds.CASH, "Cash" to AccountKind.CASH, plan.cashBalanceMinor),
            Triple(DefaultAccountIds.GOAL_FUND, "Goal fund" to AccountKind.GOAL_FUND, plan.goalFundBalanceMinor),
        ).forEach { (id, label, balance) ->
            batch.set(
                accounts.document(id),
                mapOf(
                    "name" to label.first,
                    "kind" to label.second.name,
                    "openingBalanceMinor" to balance,
                    "checkpointBalanceMinor" to balance,
                    "checkpointDate" to now.toTimestamp(),
                    "archived" to false,
                ),
                SetOptions.merge(),
            )
        }
    }

    /**
     * Committed costs become recurring items (BR-90). The first due date is the 1st of NEXT month:
     * the opening balance is already "today", so generating this month's rent would count it twice.
     */
    private fun writeCommittedCosts(batch: WriteBatch, userRef: DocumentReference, plan: OnboardingPlan, now: Instant) {
        val firstDue = now.atZone(clock.zone).toLocalDate()
            .withDayOfMonth(1).plusMonths(1)
            .atTime(LocalTime.MIDNIGHT).atZone(clock.zone).toInstant()
        val recurring = userRef.collection("recurringItems")

        listOf(
            Spec(DefaultRecurringIds.RENT, RecurringKind.RENT, "Rent", "rent", plan.rentMinor),
            Spec(DefaultRecurringIds.UTILITIES, RecurringKind.BILL, "Utilities", "utilities", plan.utilitiesMinor),
            Spec(DefaultRecurringIds.GYM, RecurringKind.BILL, "Gym", "gym", plan.gymMinor),
            Spec(DefaultRecurringIds.SUBSCRIPTIONS, RecurringKind.SUBSCRIPTION, "Subscriptions", "subscriptions", plan.subscriptionsMinor),
        ).filter { it.amountMinor > 0L }.forEach { spec ->
            val item = RecurringItem(
                id = spec.id,
                kind = spec.kind,
                name = spec.name,
                amountMinor = spec.amountMinor,
                currency = CurrencyCode.LKR,
                frequency = RecurringFrequency.MONTHLY,
                nextDueDate = firstDue,
                categoryId = spec.categoryId, // ids from CategorySeed (BR-26)
                accountId = DefaultAccountIds.BANK_MAIN,
                active = true,
            )
            batch.set(recurring.document(item.id), item.toDto())
        }
    }

    private data class Spec(
        val id: String,
        val kind: RecurringKind,
        val name: String,
        val categoryId: String,
        val amountMinor: Long,
    )
}
