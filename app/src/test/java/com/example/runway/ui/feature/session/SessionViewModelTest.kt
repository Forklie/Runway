package com.example.runway.ui.feature.session

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.AuthUser
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authFlow = MutableStateFlow<AuthUser?>(null)
    private val profileFlow = MutableStateFlow<UserProfile?>(null)
    private val auth = mockk<AuthRepository>()
    private val profiles = mockk<UserProfileRepository>()

    private val user = AuthUser(uid = "u1", email = "kavindu@example.com", displayName = "Kavindu")

    private fun profile(onboardingComplete: Boolean) = UserProfile(
        uid = "u1",
        displayName = "Kavindu",
        email = "kavindu@example.com",
        onboardingComplete = onboardingComplete,
        createdAt = Instant.EPOCH,
    )

    @Before
    fun setUp() {
        every { auth.authState } returns authFlow
        every { profiles.observeProfile() } returns profileFlow
        coEvery { auth.ensureProfile() } returns AppResult.Success(Unit)
    }

    private fun viewModel() = SessionViewModel(auth, profiles)

    /** Skips the initial Loading value. */
    private suspend fun ReceiveTurbine<SessionState>.awaitResolved(): SessionState {
        var item = awaitItem()
        while (item == SessionState.Loading) item = awaitItem()
        return item
    }

    @Test
    fun `nobody signed in goes to login`() = runTest {
        viewModel().state.test {
            assertEquals(SessionState.LoggedOut, awaitResolved())
        }
    }

    @Test
    fun `signed in before onboarding needs onboarding`() = runTest {
        authFlow.value = user
        profileFlow.value = profile(onboardingComplete = false)
        viewModel().state.test {
            assertEquals(SessionState.NeedsOnboarding, awaitResolved())
        }
    }

    @Test
    fun `signed in after onboarding is ready`() = runTest {
        authFlow.value = user
        profileFlow.value = profile(onboardingComplete = true)
        viewModel().state.test {
            assertEquals(SessionState.Ready, awaitResolved())
        }
    }

    @Test
    fun `a missing profile is repaired once, then the new doc decides the route`() = runTest {
        authFlow.value = user
        profileFlow.value = null
        viewModel().state.test {
            assertEquals(SessionState.Loading, awaitItem()) // waiting for the seeded doc
            profileFlow.value = profile(onboardingComplete = false)
            assertEquals(SessionState.NeedsOnboarding, awaitItem())
        }
        coVerify(exactly = 1) { auth.ensureProfile() }
    }

    @Test
    fun `a failed repair is a problem with the reason`() = runTest {
        coEvery { auth.ensureProfile() } returns AppResult.Failure(AppError.Network)
        authFlow.value = user
        viewModel().state.test {
            assertEquals(SessionState.Problem(AppError.Network), awaitResolved())
        }
    }

    @Test
    fun `try again repairs again and recovers`() = runTest {
        coEvery { auth.ensureProfile() } returnsMany listOf(
            AppResult.Failure(AppError.Network),
            AppResult.Success(Unit),
        )
        authFlow.value = user
        val vm = viewModel()
        vm.state.test {
            assertEquals(SessionState.Problem(AppError.Network), awaitResolved())
            vm.retry()
            profileFlow.value = profile(onboardingComplete = false)
            assertEquals(SessionState.NeedsOnboarding, awaitItem())
        }
        coVerify(exactly = 2) { auth.ensureProfile() }
    }

    @Test
    fun `signing out goes back to login`() = runTest {
        authFlow.value = user
        profileFlow.value = profile(onboardingComplete = true)
        viewModel().state.test {
            assertEquals(SessionState.Ready, awaitResolved())
            authFlow.value = null
            assertEquals(SessionState.LoggedOut, awaitItem())
        }
    }
}
