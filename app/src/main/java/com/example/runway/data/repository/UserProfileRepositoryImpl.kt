package com.example.runway.data.repository

import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.remote.mapper.toDto
import com.example.runway.data.remote.mapper.toUserProfileOrNull
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * users/{uid}. Single-field updates use set(..., merge) so they also work when the doc is missing
 * (the mapper's safe defaults fill the gaps) and never overwrite the other fields.
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider,
) : UserProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> = callbackFlow {
        val registration = doc().addSnapshotListener { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snap?.toUserProfileOrNull())
        }
        awaitClose { registration.remove() }
    }.flowOn(dispatchers.io)

    override suspend fun save(profile: UserProfile): AppResult<Unit> = appResultOf {
        doc().set(profile.toDto(), SetOptions.merge()).logWriteFailure("profile save")
    }

    override suspend fun setDisplayName(name: String): AppResult<Unit> =
        merge("displayName" to name)

    override suspend fun setOnboardingComplete(complete: Boolean): AppResult<Unit> =
        merge("onboardingComplete" to complete)

    override suspend fun setPyfPercent(percent: Double): AppResult<Unit> =
        merge("pyfPercent" to percent)

    override suspend fun setDefaultAccount(accountId: String?): AppResult<Unit> =
        merge("defaultAccountId" to accountId)

    override suspend fun setLastFxRate(currency: CurrencyCode, rate: Double): AppResult<Unit> =
        merge("lastFxRates" to mapOf(currency.name to rate)) // merge keeps the other currencies

    private fun merge(field: Pair<String, Any?>): AppResult<Unit> = appResultOf {
        doc().set(mapOf(field), SetOptions.merge()).logWriteFailure("profile ${field.first}")
    }

    private fun doc(): DocumentReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("No signed-in user")
        return firestore.collection("users").document(uid)
    }
}
