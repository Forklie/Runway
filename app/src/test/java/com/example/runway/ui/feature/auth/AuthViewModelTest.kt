package com.example.runway.ui.feature.auth

import androidx.lifecycle.SavedStateHandle
import com.example.runway.R
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.AuthUser
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.validation.EmailIssue
import com.example.runway.domain.validation.NameIssue
import com.example.runway.domain.validation.PasswordIssue
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<AuthRepository>()
    private val user = AuthUser(uid = "u1", email = "kavindu@example.com", displayName = "Kavindu")

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) = AuthViewModel(repository, saved)

    private fun AuthViewModel.fillLogin(email: String = "kavindu@example.com", password: String = "secret1") {
        onEmailChange(email)
        onPasswordChange(password)
    }

    // ---- sign in ----

    @Test
    fun `sign in with an empty email shows an inline issue and calls nothing`() {
        val vm = viewModel()
        vm.onPasswordChange("secret1")
        vm.onSignIn()

        assertEquals(EmailIssue.Empty, vm.state.value.emailIssue)
        assertFalse(vm.state.value.submitting)
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun `sign in with a malformed email is invalid`() {
        val vm = viewModel()
        vm.fillLogin(email = "kavindu")
        vm.onSignIn()

        assertEquals(EmailIssue.Invalid, vm.state.value.emailIssue)
        coVerify(exactly = 0) { repository.signIn(any(), any()) }
    }

    @Test
    fun `sign in with an empty password shows an inline issue`() {
        val vm = viewModel()
        vm.onEmailChange("kavindu@example.com")
        vm.onSignIn()

        assertEquals(PasswordIssue.Empty, vm.state.value.passwordIssue)
    }

    @Test
    fun `sign in sends the trimmed email and stays busy until the session changes`() {
        coEvery { repository.signIn(any(), any()) } returns AppResult.Success(user)
        val vm = viewModel()
        vm.fillLogin(email = "  kavindu@example.com ")
        vm.onSignIn()

        coVerify(exactly = 1) { repository.signIn("kavindu@example.com", "secret1") }
        assertTrue(vm.state.value.submitting)
    }

    @Test
    fun `wrong password keeps the input and shows an error (AT-20)`() {
        coEvery { repository.signIn(any(), any()) } returns AppResult.Failure(AppError.InvalidCredentials)
        val vm = viewModel()
        vm.fillLogin()
        vm.onSignIn()

        val state = vm.state.value
        assertEquals(AppError.InvalidCredentials, state.formError)
        assertEquals("kavindu@example.com", state.email)
        assertEquals("secret1", state.password)
        assertFalse(state.submitting)
    }

    @Test
    fun `no network keeps the input and shows an error (AT-20)`() {
        coEvery { repository.signIn(any(), any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.fillLogin()
        vm.onSignIn()

        assertEquals(AppError.Network, vm.state.value.formError)
        assertEquals("secret1", vm.state.value.password)
    }

    @Test
    fun `a double tap sends one request`() {
        val gate = CompletableDeferred<AppResult<AuthUser>>()
        coEvery { repository.signIn(any(), any()) } coAnswers { gate.await() }
        val vm = viewModel()
        vm.fillLogin()
        vm.onSignIn()
        vm.onSignIn()

        coVerify(exactly = 1) { repository.signIn(any(), any()) }
        gate.complete(AppResult.Failure(AppError.Network))
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun `typing clears the issue and the error`() {
        coEvery { repository.signIn(any(), any()) } returns AppResult.Failure(AppError.InvalidCredentials)
        val vm = viewModel()
        vm.fillLogin()
        vm.onSignIn()
        assertEquals(AppError.InvalidCredentials, vm.state.value.formError)

        vm.onPasswordChange("secret12")
        assertNull(vm.state.value.formError)
    }

    // ---- register ----

    @Test
    fun `register checks name, email and a 6 character password before calling`() {
        val vm = viewModel()
        vm.onPasswordChange("12345")
        vm.onRegister()

        val state = vm.state.value
        assertEquals(NameIssue.Empty, state.nameIssue)
        assertEquals(EmailIssue.Empty, state.emailIssue)
        assertEquals(PasswordIssue.TooShort, state.passwordIssue)
        coVerify(exactly = 0) { repository.register(any(), any(), any()) }
    }

    @Test
    fun `register sends trimmed name and email`() {
        coEvery { repository.register(any(), any(), any()) } returns AppResult.Success(user)
        val vm = viewModel()
        vm.onNameChange(" Kavindu ")
        vm.onEmailChange(" kavindu@example.com ")
        vm.onPasswordChange("secret1")
        vm.onRegister()

        coVerify(exactly = 1) { repository.register("kavindu@example.com", "secret1", "Kavindu") }
    }

    @Test
    fun `duplicate email is shown under the email field and the input is kept (AT-20)`() {
        coEvery { repository.register(any(), any(), any()) } returns AppResult.Failure(AppError.EmailInUse)
        val vm = viewModel()
        vm.onNameChange("Kavindu")
        vm.onEmailChange("kavindu@example.com")
        vm.onPasswordChange("secret1")
        vm.onRegister()

        val state = vm.state.value
        assertEquals(AppError.EmailInUse, state.formError)
        assertEquals(com.example.runway.R.string.error_email_in_use, state.emailErrorRes())
        assertNull(state.bannerErrorRes())
        assertEquals("Kavindu", state.name)
        assertEquals("secret1", state.password)
    }

    // ---- reset ----

    @Test
    fun `reset with a bad email shows an inline issue and sends nothing`() {
        val vm = viewModel()
        vm.onEmailChange("nope")
        vm.onSendReset()

        assertEquals(EmailIssue.Invalid, vm.state.value.emailIssue)
        coVerify(exactly = 0) { repository.sendPasswordReset(any()) }
    }

    @Test
    fun `reset success shows the neutral message`() {
        coEvery { repository.sendPasswordReset(any()) } returns AppResult.Success(Unit)
        val vm = viewModel()
        vm.onEmailChange("kavindu@example.com")
        vm.onSendReset()

        assertTrue(vm.state.value.resetRequested)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun `reset for an unknown email looks exactly the same`() {
        coEvery { repository.sendPasswordReset(any()) } returns AppResult.Failure(AppError.InvalidCredentials)
        val vm = viewModel()
        vm.onEmailChange("nobody@example.com")
        vm.onSendReset()

        assertTrue(vm.state.value.resetRequested)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun `reset offline is an error, not the neutral message`() {
        coEvery { repository.sendPasswordReset(any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.onEmailChange("kavindu@example.com")
        vm.onSendReset()

        assertFalse(vm.state.value.resetRequested)
        assertEquals(AppError.Network, vm.state.value.formError)
    }

    // ---- Google sign-in (F-18) ----

    @Test
    fun `tapping Google locks the screen and clears an old error`() {
        val vm = viewModel()
        assertTrue(vm.onGoogleStarted())
        assertTrue(vm.state.value.submitting)
        assertNull(vm.state.value.formError)
    }

    @Test
    fun `a second tap while Google is open is ignored`() {
        val vm = viewModel()
        assertTrue(vm.onGoogleStarted())
        assertFalse(vm.onGoogleStarted())
    }

    @Test
    fun `a Google token is sent to the repository once`() {
        coEvery { repository.signInWithGoogle("token-123") } returns AppResult.Success(user)
        val vm = viewModel()
        vm.onGoogleStarted()
        vm.onGoogleToken("token-123")

        coVerify(exactly = 1) { repository.signInWithGoogle("token-123") }
        assertTrue(vm.state.value.submitting) // stays busy: the session is about to change
    }

    @Test
    fun `a failed Google sign-in shows the error and unlocks`() {
        coEvery { repository.signInWithGoogle(any()) } returns AppResult.Failure(AppError.Network)
        val vm = viewModel()
        vm.onGoogleStarted()
        vm.onGoogleToken("token-123")

        assertFalse(vm.state.value.submitting)
        assertEquals(AppError.Network, vm.state.value.formError)
    }

    @Test
    fun `closing the Google sheet unlocks quietly`() {
        val vm = viewModel()
        vm.onGoogleStarted()
        vm.onGoogleCancelled()

        assertFalse(vm.state.value.submitting)
        assertNull(vm.state.value.formError)
        assertFalse(vm.state.value.googleUnavailable)
        assertNull(vm.state.value.bannerErrorRes())
    }

    @Test
    fun `Google not available shows its own message and keeps email sign-in usable`() {
        val vm = viewModel()
        vm.onEmailChange("kavindu@example.com")
        vm.onGoogleStarted()
        vm.onGoogleUnavailable()

        assertFalse(vm.state.value.submitting)
        assertTrue(vm.state.value.googleUnavailable)
        assertEquals(R.string.auth_google_unavailable, vm.state.value.bannerErrorRes())
        assertEquals("kavindu@example.com", vm.state.value.email) // input kept
    }

    @Test
    fun `trying Google again clears the unavailable message`() {
        val vm = viewModel()
        vm.onGoogleStarted()
        vm.onGoogleUnavailable()
        vm.onGoogleStarted()
        assertFalse(vm.state.value.googleUnavailable)
    }

    // ---- process death ----

    @Test
    fun `name and email come back from saved state, the password does not`() {
        val saved = SavedStateHandle()
        val first = viewModel(saved)
        first.onNameChange("Kavindu")
        first.onEmailChange("kavindu@example.com")
        first.onPasswordChange("secret1")

        val restored = viewModel(saved).state.value
        assertEquals("Kavindu", restored.name)
        assertEquals("kavindu@example.com", restored.email)
        assertEquals("", restored.password)
    }
}
