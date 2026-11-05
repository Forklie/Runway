package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toInstantOr
import com.example.runway.data.remote.mapper.toTimestamp
import com.example.runway.domain.model.DefaultAccountIds
import com.example.runway.domain.model.DefaultGoalIds
import com.example.runway.domain.model.GoalSettings
import com.example.runway.domain.repository.GoalSettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * users/{uid}/goals/main. Reads with a live listener, writes with merge so C's other fields survive.
 * When C's GoalRepository is on develop this can be swapped for it: only this file changes.
 */
@Singleton
class GoalSettingsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : GoalSettingsRepository {

    override fun observeGoal(): Flow<GoalSettings?> = callbackFlow {
        val registration = doc().addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.toGoalSettingsOrNull())
        }
        awaitClose { registration.remove() } // never leak listeners
    }.flowOn(dispatchers.io)

    override suspend fun updateGoal(name: String, targetMinor: Long, deadline: Instant?): AppResult<Unit> =
        appResultOf {
            val fields = buildMap<String, Any> {
                put("name", name)
                put("targetMinor", targetMinor)
                if (deadline != null) put("deadline", deadline.toTimestamp())
            }
            doc().set(fields, SetOptions.merge()).logWriteFailure("goal main")
        }

    /** A missing or malformed goal is null, never a crash. */
    private fun DocumentSnapshot.toGoalSettingsOrNull(): GoalSettings? {
        if (!exists()) return null
        val target = getLong("targetMinor") ?: return null
        return GoalSettings(
            name = getString("name").orEmpty().ifBlank { "My goal" },
            targetMinor = target,
            startDate = getTimestamp("startDate").toInstantOr(),
            deadline = getTimestamp("deadline").toInstantOr(),
            fundAccountId = getString("fundAccountId") ?: DefaultAccountIds.GOAL_FUND,
            active = getBoolean("active") ?: true,
        )
    }

    private fun doc(): DocumentReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("goals").document(DefaultGoalIds.MAIN)
    }
}
