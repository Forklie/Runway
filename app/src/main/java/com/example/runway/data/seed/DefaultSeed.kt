package com.example.runway.data.seed

import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.DefaultAccountIds
import com.example.runway.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything a brand-new user needs, written as ONE batch (atomic, and it queues offline):
 * users/{uid}, the default accounts and the BR-26 categories.
 * Ids are fixed, so running it twice overwrites the same documents instead of duplicating them.
 */
@Singleton
class DefaultSeed @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val clock: Clock,
) {

    /** Returns after the LOCAL write is queued. The caller never waits for the server. */
    fun seedNewUser(uid: String, email: String, displayName: String) {
        val now = clock.instant()
        val userRef = firestore.collection("users").document(uid)
        val batch = firestore.batch()

        val profile = UserProfile(
            uid = uid,
            displayName = displayName,
            email = email,
            defaultAccountId = DefaultAccountIds.BANK_MAIN,
            createdAt = now,
        )
        batch.set(userRef, profile.toDto())

        defaultAccounts(now).forEach { account ->
            batch.set(userRef.collection("accounts").document(account.id), account.toDto())
        }

        DEFAULT_CATEGORIES.forEachIndexed { index, category ->
            batch.set(userRef.collection("categories").document(category.id), category.toFields(index))
        }

        batch.commit().logWriteFailure("seed users/$uid")
    }

    /** createdAt is staggered by 1 ms so the accounts list keeps this order. */
    private fun defaultAccounts(now: Instant): List<Account> = listOf(
        account(DefaultAccountIds.BANK_MAIN, "Main bank", AccountKind.BANK, now),
        account(DefaultAccountIds.CASH, "Cash", AccountKind.CASH, now.plusMillis(1)),
        account(DefaultAccountIds.GOAL_FUND, "Goal fund", AccountKind.GOAL_FUND, now.plusMillis(2)),
    )

    private fun account(id: String, name: String, kind: AccountKind, at: Instant) = Account(
        id = id,
        name = name,
        kind = kind,
        openingBalanceMinor = 0L,
        checkpointBalanceMinor = 0L,
        checkpointDate = at,
        archived = false,
        createdAt = at,
    )
}
