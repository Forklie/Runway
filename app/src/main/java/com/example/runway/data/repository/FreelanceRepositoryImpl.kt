package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toFreelanceProjectOrNull
import com.example.runway.data.remote.mapper.toMilestoneOrNull
import com.example.runway.domain.model.FreelanceProject
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.repository.FreelanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** users/{uid}/freelanceProjects and users/{uid}/milestones. Small lists: sorted on the client. */
@Singleton
class FreelanceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : FreelanceRepository {

    override fun observeProjects(): Flow<List<FreelanceProject>> =
        observe("freelanceProjects") { docs ->
            docs.mapNotNull { it.toFreelanceProjectOrNull() }.sortedByDescending { it.createdAt }
        }

    override fun observeMilestones(): Flow<List<Milestone>> =
        observe("milestones") { docs -> docs.mapNotNull { it.toMilestoneOrNull() } }

    override suspend fun saveProject(project: FreelanceProject): AppResult<Unit> = appResultOf {
        col("freelanceProjects").document(project.id).set(project.toDto()).logWriteFailure("project ${project.id}")
    }

    override suspend fun saveMilestone(milestone: Milestone): AppResult<Unit> = appResultOf {
        col("milestones").document(milestone.id).set(milestone.toDto()).logWriteFailure("milestone ${milestone.id}")
    }

    override suspend fun markPaid(milestone: Milestone, income: Transaction): AppResult<Unit> = appResultOf {
        val batch = firestore.batch()
        batch.set(col("milestones").document(milestone.id), milestone.toDto())
        batch.set(col("transactions").document(income.id), income.toDto())
        batch.commit().logWriteFailure("milestone paid ${milestone.id}")
    }

    private fun <T> observe(collection: String, map: (List<DocumentSnapshot>) -> List<T>): Flow<List<T>> = callbackFlow {
        val registration = col(collection).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(map(snap?.documents.orEmpty()))
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    private fun col(name: String): CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid).collection(name)
    }
}
