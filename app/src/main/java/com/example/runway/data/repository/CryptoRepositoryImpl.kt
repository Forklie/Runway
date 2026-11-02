package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toCryptoEntryOrNull
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.repository.CryptoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** users/{uid}/cryptoEntries: the net-position ledger (F-17). */
@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : CryptoRepository {

    override fun observeEntries(): Flow<List<CryptoEntry>> = callbackFlow {
        val registration = col().orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.documents?.mapNotNull { it.toCryptoEntryOrNull() }.orEmpty())
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun add(entry: CryptoEntry): AppResult<Unit> = appResultOf {
        col().document(entry.id).set(entry.toDto()).logWriteFailure("crypto entry ${entry.id}")
    }

    override suspend fun remove(id: String): AppResult<Unit> = appResultOf {
        col().document(id).delete().logWriteFailure("remove crypto entry $id")
    }

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("cryptoEntries")
    }
}
