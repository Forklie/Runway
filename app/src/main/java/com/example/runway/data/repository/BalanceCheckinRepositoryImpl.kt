package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toBalanceCheckinOrNull
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toTimestamp
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.repository.BalanceCheckinRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * users/{uid}/balanceCheckins. Saving also moves each account's checkpoint (users/{uid}/accounts),
 * in the same batch, so a check-in and its new checkpoint can never land apart (Doc 2 section 5.8).
 */
@Singleton
class BalanceCheckinRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : BalanceCheckinRepository {

    override fun observeAll(): Flow<List<BalanceCheckin>> = callbackFlow {
        val registration = userDoc().collection(CHECKINS)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(RECENT_LIMIT)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toBalanceCheckinOrNull() }.orEmpty())
            }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun saveCheckin(checkins: List<BalanceCheckin>): AppResult<Unit> = appResultOf {
        val user = userDoc()
        val batch = firestore.batch()
        checkins.forEach { checkin ->
            batch.set(user.collection(CHECKINS).document(checkin.id), checkin.toDto())
            batch.update(
                user.collection("accounts").document(checkin.accountId),
                mapOf(
                    "checkpointBalanceMinor" to checkin.enteredBalanceMinor,
                    "checkpointDate" to checkin.date.toTimestamp(),
                ),
            )
        }
        batch.commit().logWriteFailure("check-in batch (${checkins.size})")
    }

    private fun userDoc(): DocumentReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid)
    }

    private companion object {
        const val CHECKINS = "balanceCheckins"
        const val RECENT_LIMIT = 120L // about six months of weekly check-ins on four accounts
    }
}
