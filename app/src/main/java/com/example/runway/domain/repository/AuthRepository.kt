package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Current user (or null) first, then on every sign-in and sign-out. */
    val authState: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): AppResult<AuthUser>

    /** Creates the account, then seeds users/{uid}, categories and accounts in ONE batch. */
    suspend fun register(email: String, password: String, displayName: String): AppResult<AuthUser>

    suspend fun sendPasswordReset(email: String): AppResult<Unit>
    suspend fun signInWithGoogle(idToken: String): AppResult<AuthUser>

    /**
     * Repair for a signed-in user whose users/{uid} doc is missing (app killed between sign-up and
     * the seed batch, or a first Google sign-in). Asks the SERVER once and seeds only if the doc is
     * really missing. Fails with Network when offline, so a cached "missing" never overwrites data.
     */
    suspend fun ensureProfile(): AppResult<Unit>

    fun signOut()
}
