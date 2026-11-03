package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toGoalOrNull
import com.example.runway.domain.model.Goal
import com.example.runway.domain.repository.GoalRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** users/{uid}/goals. One equality filter, so the automatic single-field index is enough. */
@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : GoalRepository {

    override fun observeActiveGoal(): Flow<Goal?> = callbackFlow {
        val registration = col().whereEqualTo("active", true).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            // v1 has one active goal (BR-70); if a second ever slips in, the newest wins.
            trySend(snap?.documents?.mapNotNull { it.toGoalOrNull() }?.maxByOrNull { it.startDate })
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun save(goal: Goal): AppResult<Unit> = appResultOf {
        col().document(goal.id).set(goal.toDto()).logWriteFailure("goal ${goal.id}")
    }

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("goals")
    }
}
