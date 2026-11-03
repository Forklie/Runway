package com.example.runway.ui.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.validation.CredentialValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One ViewModel class for Login, Register and Reset (each screen gets its own instance).
 * - Validates on submit, never per keystroke, and keeps what the user typed.
 * - Name and email survive process death through SavedStateHandle. The password never does.
 * - On success there is nothing to navigate to: authState changes, SessionViewModel routes.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthFormState(
            name = savedState[KEY_NAME] ?: "",
            email = savedState[KEY_EMAIL] ?: "",
        ),
    )
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun onNameChange(value: String) {
        savedState[KEY_NAME] = value
        _state.update { it.copy(name = value, nameIssue = null, formError = null) }
    }

    fun onEmailChange(value: String) {
        savedState[KEY_EMAIL] = value
        _state.update { it.copy(email = value, emailIssue = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordIssue = null, formError = null) }
    }

    fun onSignIn() {
        val form = _state.value
        if (form.submitting) return // a double tap must not send two requests
        val emailIssue = CredentialValidator.validateEmail(form.email)
        val passwordIssue = CredentialValidator.validateSignInPassword(form.password)
        if (emailIssue != null || passwordIssue != null) {
            _state.update { it.copy(emailIssue = emailIssue, passwordIssue = passwordIssue) }
            return
        }
        submit { authRepository.signIn(form.email.trim(), form.password) }
    }

    fun onRegister() {
        val form = _state.value
        if (form.submitting) return
        val nameIssue = CredentialValidator.validateName(form.name)
        val emailIssue = CredentialValidator.validateEmail(form.email)
        val passwordIssue = CredentialValidator.validateNewPassword(form.password)
        if (nameIssue != null || emailIssue != null || passwordIssue != null) {
            _state.update {
                it.copy(nameIssue = nameIssue, emailIssue = emailIssue, passwordIssue = passwordIssue)
            }
            return
        }
        submit { authRepository.register(form.email.trim(), form.password, form.name.trim()) }
    }

    /**
     * The message is the same whether or not the email has an account ("If that email exists...").
     * Only a network failure is shown as an error, because then nothing was sent.
     */
    fun onSendReset() {
        val form = _state.value
        if (form.submitting) return
        val emailIssue = CredentialValidator.validateEmail(form.email)
        if (emailIssue != null) {
            _state.update { it.copy(emailIssue = emailIssue) }
            return
        }
        _state.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(form.email.trim())
            _state.update {
                when {
                    result is AppResult.Success ||
                        (result is AppResult.Failure && result.error == AppError.InvalidCredentials) ->
                        it.copy(submitting = false, resetRequested = true)

                    result is AppResult.Failure -> it.copy(submitting = false, formError = result.error)
                    else -> it.copy(submitting = false)
                }
            }
        }
    }

    // ---- Google sign-in (F-18). The Android part lives in GoogleSignInHelper: the ViewModel only sees the token. ----

    /** The button was tapped. Locks the screen while Google's sheet is open. False if already busy (a double tap). */
    fun onGoogleStarted(): Boolean {
        if (_state.value.submitting) return false
        _state.update { it.copy(submitting = true, formError = null, googleUnavailable = false) }
        return true
    }

    /** Hands the Google token to Firebase. ensureProfile() creates the profile on a first Google sign-in. */
    fun onGoogleToken(idToken: String) {
        submit { authRepository.signInWithGoogle(idToken) }
    }

    /** The person closed the sheet: unlock quietly, nothing to say. */
    fun onGoogleCancelled() {
        _state.update { it.copy(submitting = false) }
    }

    /** The sheet could not open: unlock and point to email sign-in instead. */
    fun onGoogleUnavailable() {
        _state.update { it.copy(submitting = false, googleUnavailable = true) }
    }

    private fun submit(call: suspend () -> AppResult<*>) {
        _state.update { it.copy(submitting = true, formError = null, googleUnavailable = false) }
        viewModelScope.launch {
            when (val result = call()) {
                // Stay "busy": the screen is about to be replaced when the session changes.
                is AppResult.Success -> Unit
                is AppResult.Failure -> _state.update { it.copy(submitting = false, formError = result.error) }
            }
        }
    }

    private companion object {
        const val KEY_NAME = "auth_name"
        const val KEY_EMAIL = "auth_email"
    }
}
