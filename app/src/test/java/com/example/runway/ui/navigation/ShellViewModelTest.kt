package com.example.runway.ui.navigation

import com.example.runway.core.network.ConnectivityObserver
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.domain.usecase.GenerateRecurring
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.feature.session.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ShellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val connectivity = mockk<ConnectivityObserver>()
    private val generate = mockk<GenerateRecurring>()

    @Before
    fun setUp() {
        every { connectivity.isOnline } returns flowOf(true)
        coEvery { generate.invoke() } returns AppResult.Success(0)
    }

    private fun viewModel() = ShellViewModel(connectivity, generate)

    @Test
    fun `generation runs when the session becomes ready`() {
        viewModel().onSessionChanged(SessionState.Ready)
        coVerify(exactly = 1) { generate.invoke() }
    }

    @Test
    fun `it runs once even if the session re-emits ready`() {
        val vm = viewModel()
        vm.onSessionChanged(SessionState.Ready)
        vm.onSessionChanged(SessionState.Ready)
        vm.onSessionChanged(SessionState.Ready)
        coVerify(exactly = 1) { generate.invoke() }
    }

    @Test
    fun `it does not run while loading, in onboarding or on a problem`() {
        val vm = viewModel()
        vm.onSessionChanged(SessionState.Loading)
        vm.onSessionChanged(SessionState.NeedsOnboarding)
        vm.onSessionChanged(SessionState.Problem(AppError.Network))
        coVerify(exactly = 0) { generate.invoke() }
    }

    @Test
    fun `a problem in between does not re-arm it`() {
        val vm = viewModel()
        vm.onSessionChanged(SessionState.Ready)
        vm.onSessionChanged(SessionState.Problem(AppError.Network))
        vm.onSessionChanged(SessionState.Ready)
        coVerify(exactly = 1) { generate.invoke() }
    }

    @Test
    fun `signing out and back in runs it again`() {
        val vm = viewModel()
        vm.onSessionChanged(SessionState.Ready)
        vm.onSessionChanged(SessionState.LoggedOut)
        vm.onSessionChanged(SessionState.Ready)
        coVerify(exactly = 2) { generate.invoke() }
    }

    @Test
    fun `a failed generation does not crash the app`() {
        coEvery { generate.invoke() } returns AppResult.Failure(AppError.Network)
        viewModel().onSessionChanged(SessionState.Ready)
        coVerify(exactly = 1) { generate.invoke() }
    }
}
