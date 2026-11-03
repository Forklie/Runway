package com.example.runway.ui.feature.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.runway.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import timber.log.Timber

/** What the Google account sheet gave back. Nothing here knows about Firebase. */
sealed interface GoogleSignInResult {
    /** A Google ID token. AuthRepository.signInWithGoogle(idToken) turns it into a Firebase session (FR-22). */
    data class Token(val idToken: String) : GoogleSignInResult

    /** The person closed the sheet. Not an error: nothing to show. */
    data object Cancelled : GoogleSignInResult

    /** No Google account on the phone, no Play services, or the app's SHA-1 / client id is not set up. */
    data object Unavailable : GoogleSignInResult
}

/**
 * Asks Android's Credential Manager for a Google ID token (F-18). It needs an ACTIVITY context to draw
 * the account sheet, so it lives in the UI layer and is never handed to a ViewModel: the ViewModel only
 * ever receives the token string.
 *
 * [webClientId] is the WEB client id (client_type 3 in google-services.json), not the Android one.
 */
class GoogleSignInHelper(private val webClientId: String) {

    suspend fun requestIdToken(activityContext: Context): GoogleSignInResult {
        // The "Sign in with Google" option is the one meant for a button: it works for new and returning users.
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
                .credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Timber.w("Unexpected credential type %s", credential.type)
                GoogleSignInResult.Unavailable
            }
        } catch (e: GetCredentialCancellationException) { // must come before its parent class
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            Timber.w(e, "Google sign-in unavailable")
            GoogleSignInResult.Unavailable
        } catch (e: GoogleIdTokenParsingException) {
            Timber.w(e, "Google token could not be read")
            GoogleSignInResult.Unavailable
        }
    }
}

/**
 * Returns the click handler for "Continue with Google". Shared by Login and Register so the flow
 * exists once: lock the screen, ask Google, hand the token to the ViewModel.
 * If the screen leaves composition mid-flow, rememberCoroutineScope cancels the request.
 */
@Composable
fun rememberGoogleSignIn(viewModel: AuthViewModel): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm by rememberUpdatedState(viewModel)
    val helper = remember(context) { GoogleSignInHelper(context.getString(R.string.default_web_client_id)) }
    return remember(context) {
        {
            if (vm.onGoogleStarted()) {
                scope.launch {
                    when (val result = helper.requestIdToken(context)) {
                        is GoogleSignInResult.Token -> vm.onGoogleToken(result.idToken)
                        GoogleSignInResult.Cancelled -> vm.onGoogleCancelled()
                        GoogleSignInResult.Unavailable -> vm.onGoogleUnavailable()
                    }
                }
            }
        }
    }
}
