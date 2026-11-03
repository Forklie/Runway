package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toIncomeSourceOrNull
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.repository.IncomeSourceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** users/{uid}/incomeSources: salary, freelance, AdSense, crypto, other. */
@Singleton
class IncomeSourceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : IncomeSourceRepository {

    override fun observeSources(): Flow<List<IncomeSource>> = callbackFlow {
        val registration = col().addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toIncomeSourceOrNull() }
                ?.sortedWith(compareBy({ it.reliability }, { it.name })) // reliable (salary) first
                .orEmpty()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun add(source: IncomeSource): AppResult<Unit> = appResultOf {
        col().document(source.id).set(source.toDto()).logWriteFailure("income source ${source.id}")
    }

    override suspend fun addAll(sources: List<IncomeSource>): AppResult<Unit> = appResultOf {
        val collection = col()
        val batch = firestore.batch()
        sources.forEach { batch.set(collection.document(it.id), it.toDto()) }
        batch.commit().logWriteFailure("income sources batch (${sources.size})")
    }

    override suspend fun update(source: IncomeSource): AppResult<Unit> = appResultOf {
        col().document(source.id).set(source.toDto()).logWriteFailure("income source ${source.id}")
    }

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("incomeSources")
    }
}
