package com.example.runway.data.repository

import com.example.runway.core.result.AppResult
import com.example.runway.core.result.appResultOf
import com.example.runway.data.remote.logWriteFailure
import com.example.runway.data.seed.DefaultSeed
import com.example.runway.domain.model.AuthUser
import com.example.runway.domain.repository.AuthRepository
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth wrapper. Auth calls use await() because they need the network (Doc 2 §5.8).
 * Errors go through toAppError() inside appResultOf, so the UI never sees Firebase types.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val seed: DefaultSeed,
) : AuthRepository {

    /** True while register() is creating the account and queuing the seed batch. */
    @Volatile
    private var registering = false

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener) // fires once straight away with the current user
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> = appResultOf {
        auth.signInWithEmailAndPassword(email.trim(), password).await().requireUser()
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
    ): AppResult<AuthUser> {
        registering = true // tells ensureProfile() not to seed a second time
        return try {
            appResultOf {
                val name = displayName.trim()
                val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
                    ?: throw IllegalStateException("Auth returned no user")
                seed.seedNewUser(uid = user.uid, email = user.email ?: email.trim(), displayName = name)
                // Best effort and not awaited: the name also lives on the Auth user for the repair path.
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                    .logWriteFailure("auth display name")
                user.toAuthUser().copy(displayName = name)
            }
        } finally {
            registering = false
        }
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> = appResultOf {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    /** A first-time Google user has no profile yet: ensureProfile() seeds it from the session layer. */
    override suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser> = appResultOf {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await().requireUser()
    }

    override suspend fun ensureProfile(): AppResult<Unit> = appResultOf {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        if (!registering) {
            // Server read on purpose: an empty CACHE does not prove the doc is missing (new device).
            val snapshot = firestore.collection("users").document(user.uid).get(Source.SERVER).await()
            if (!snapshot.exists()) {
                seed.seedNewUser(user.uid, user.email.orEmpty(), user.displayName.orEmpty())
            }
        }
    }

    override fun signOut() = auth.signOut()

    private fun AuthResult.requireUser(): AuthUser =
        user?.toAuthUser() ?: throw IllegalStateException("Auth returned no user")

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email, displayName = displayName)
}
