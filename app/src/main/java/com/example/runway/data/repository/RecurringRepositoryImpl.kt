package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toRecurringItemOrNull
import com.example.runway.data.remote.mapper.toTimestamp
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.repository.RecurringRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

/** users/{uid}/recurringItems. Live listeners in, local-cache writes out (never awaited). */
@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : RecurringRepository {

    override fun observeAll(): Flow<List<RecurringItem>> = callbackFlow {
        val registration = col().addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.documents?.mapNotNull { it.toRecurringItemOrNull() }.orEmpty().sortedBy { it.nextDueDate })
        }
        awaitClose { registration.remove() } // never leak listeners
    }.flowOn(dispatchers.io)

    /** One equality filter needs no composite index. Sorted on the client. */
    override fun observeSubscriptions(): Flow<List<RecurringItem>> = callbackFlow {
        val registration = col().whereEqualTo("kind", "SUBSCRIPTION").addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.documents?.mapNotNull { it.toRecurringItemOrNull() }.orEmpty().sortedBy { it.name.lowercase() })
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    /** Uses the composite index active asc + nextDueDate asc (firestore.indexes.json). */
    override suspend fun getDue(until: Instant): AppResult<List<RecurringItem>> = appResultOf {
        col().whereEqualTo("active", true)
            .whereLessThanOrEqualTo("nextDueDate", until.toTimestamp())
            .get()
            .await() // a one-off read, so awaiting is correct (Doc 2 §5.8)
            .documents
            .mapNotNull { it.toRecurringItemOrNull() }
    }

    override suspend fun add(item: RecurringItem): AppResult<Unit> = appResultOf {
        col().document(item.id).set(item.toDto()).logWriteFailure("recurring ${item.id}")
    }

    override suspend fun addAll(items: List<RecurringItem>): AppResult<Unit> = appResultOf {
        val collection = col()
        val batch = firestore.batch()
        items.forEach { batch.set(collection.document(it.id), it.toDto()) }
        batch.commit().logWriteFailure("recurring batch (${items.size})")
    }

    override suspend fun update(item: RecurringItem): AppResult<Unit> = appResultOf {
        col().document(item.id).set(item.toDto()).logWriteFailure("recurring ${item.id}")
    }

    override suspend fun setActive(id: String, active: Boolean): AppResult<Unit> = appResultOf {
        col().document(id).update("active", active).logWriteFailure("recurring active $id")
    }

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("recurringItems")
    }
}
