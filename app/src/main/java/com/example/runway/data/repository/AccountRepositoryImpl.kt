package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toAccountOrNull
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.domain.model.Account
import com.example.runway.domain.repository.AccountRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** users/{uid}/accounts. Live listeners in, local-cache writes out (never awaited). */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> = callbackFlow {
        val registration = col().addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toAccountOrNull() }
                ?.filterNot { it.archived } // archived filtered client-side, no extra index
                ?.sortedBy { it.createdAt }
                .orEmpty()
            trySend(items)
        }
        awaitClose { registration.remove() } // never leak listeners
    }.flowOn(dispatchers.io)

    override fun observeById(id: String): Flow<Account?> = callbackFlow {
        val registration = col().document(id).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.takeIf { it.exists() }?.toAccountOrNull())
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun add(account: Account): AppResult<Unit> = appResultOf {
        col().document(account.id).set(account.toDto()).logWriteFailure("account ${account.id}")
    }

    override suspend fun addAll(accounts: List<Account>): AppResult<Unit> = appResultOf {
        val collection = col()
        val batch = firestore.batch()
        accounts.forEach { batch.set(collection.document(it.id), it.toDto()) }
        batch.commit().logWriteFailure("accounts batch (${accounts.size})")
    }

    override suspend fun update(account: Account): AppResult<Unit> = appResultOf {
        col().document(account.id).set(account.toDto()).logWriteFailure("account ${account.id}")
    }

    override suspend fun archive(id: String, archived: Boolean): AppResult<Unit> = appResultOf {
        col().document(id).update("archived", archived).logWriteFailure("archive account $id")
    }

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("accounts")
    }
}
