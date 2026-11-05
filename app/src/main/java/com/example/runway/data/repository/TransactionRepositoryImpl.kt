package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toTimestamp
import com.example.runway.data.remote.mapper.toTransactionOrNull
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * users/{uid}/transactions. One collection for every money movement, so one live query feeds Home,
 * Activity and the balances. Archived rows are filtered here, client-side, so no extra index is needed.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) : TransactionRepository {

    // Composite index: monthKey ↑, date ↓
    override fun observeMonth(monthKey: String): Flow<List<Transaction>> = observeQuery {
        it.whereEqualTo("monthKey", monthKey).orderBy("date", Query.Direction.DESCENDING)
    }

    // Automatic single-field index on date
    override fun observeSince(from: Instant): Flow<List<Transaction>> = observeQuery {
        it.whereGreaterThanOrEqualTo("date", from.toTimestamp()).orderBy("date", Query.Direction.DESCENDING)
    }

    // Composite index: type ↑, date ↓
    override fun observeIncomeSince(from: Instant): Flow<List<Transaction>> = observeQuery {
        it.whereEqualTo("type", TransactionType.INCOME.name)
            .whereGreaterThanOrEqualTo("date", from.toTimestamp())
            .orderBy("date", Query.Direction.DESCENDING)
    }

    override fun observeById(id: String): Flow<Transaction?> = callbackFlow {
        val registration = col().document(id).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.takeIf { it.exists() }?.toTransactionOrNull())
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun add(tx: Transaction): AppResult<Unit> = appResultOf {
        col().document(tx.id).set(tx.toDto()).logWriteFailure("transaction ${tx.id}")
    }

    override suspend fun addAll(transactions: List<Transaction>): AppResult<Unit> = appResultOf {
        val collection = col()
        val batch = firestore.batch()
        transactions.forEach { batch.set(collection.document(it.id), it.toDto()) }
        batch.commit().logWriteFailure("transactions batch (${transactions.size})")
    }

    override suspend fun update(tx: Transaction): AppResult<Unit> = appResultOf {
        col().document(tx.id).set(tx.toDto()).logWriteFailure("transaction ${tx.id}")
    }

    /** Soft delete (BR-16). The rules deny real deletes. */
    override suspend fun archive(id: String, archived: Boolean): AppResult<Unit> = appResultOf {
        col().document(id)
            .update(mapOf("archived" to archived, "updatedAt" to clock.instant().toTimestamp()))
            .logWriteFailure("archive transaction $id")
    }

    private fun observeQuery(build: (CollectionReference) -> Query): Flow<List<Transaction>> = callbackFlow {
        val registration = build(col()).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toTransactionOrNull() }
                ?.filterNot { it.archived }
                .orEmpty()
            trySend(items)
        }
        awaitClose { registration.remove() } // never leak listeners
    }.flowOn(dispatchers.io)

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("transactions")
    }
}
