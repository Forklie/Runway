package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toCategoryOrNull
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.domain.model.Category
import com.example.runway.domain.repository.CategoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** users/{uid}/categories. A handful of documents, so sorting and filtering happen on the client. */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val registration = col().addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toCategoryOrNull() }
                ?.filterNot { it.archived }
                ?.sortedBy { it.sortOrder }
                .orEmpty()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun add(category: Category): AppResult<Unit> = appResultOf {
        col().document(category.id).set(category.toDto()).logWriteFailure("category ${category.id}")
    }

    override suspend fun update(category: Category): AppResult<Unit> = appResultOf {
        col().document(category.id).set(category.toDto()).logWriteFailure("category ${category.id}")
    }

    override suspend fun archive(id: String, archived: Boolean): AppResult<Unit> = appResultOf {
        col().document(id).update("archived", archived).logWriteFailure("archive category $id")
    }

    private fun col(): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection("categories")
    }
}
