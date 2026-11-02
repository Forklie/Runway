package com.example.runway.ui.feature.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.runway.core.network.ConnectivityObserver
import com.example.runway.domain.usecase.HomeSummary
import com.example.runway.domain.usecase.ObserveHomeSummary
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeHome = mockk<ObserveHomeSummary>()
    private val connectivity = mockk<ConnectivityObserver>()
    private val summary = previewHomeSummary()

    @Before
    fun setUp() {
        every { connectivity.isOnline } returns flowOf(true)
    }

    private fun viewModel() = HomeViewModel(observeHome, connectivity)

    /** Skips Loading values. */
    private suspend fun ReceiveTurbine<UiState<HomeSummary>>.awaitResolved(): UiState<HomeSummary> {
        var item = awaitItem()
        while (item == UiState.Loading) item = awaitItem()
        return item
    }

    @Test
    fun `a live summary is shown as Success`() = runTest {
        every { observeHome() } returns flowOf(summary)
        viewModel().uiState.test {
            assertEquals(UiState.Success(summary), awaitResolved())
        }
    }

    @Test
    fun `nothing seeded yet is the Empty state, not a crash (AT-01)`() = runTest {
        every { observeHome() } returns flowOf(summary.copy(isEmpty = true))
        viewModel().uiState.test {
            assertEquals(UiState.Empty, awaitResolved())
        }
    }

    @Test
    fun `a listener failure is an Error, and Try again recovers (Z9)`() = runTest {
        every { observeHome() } returnsMany listOf(
            flow { throw IllegalStateException("No signed-in user") },
            flowOf(summary),
        )
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(awaitResolved() is UiState.Error)
            vm.retry()
            assertEquals(UiState.Success(summary), awaitResolved())
        }
    }

    @Test
    fun `offline drives the saved-on-this-phone chip`() = runTest {
        every { observeHome() } returns flowOf(summary)
        every { connectivity.isOnline } returns flowOf(false)
        viewModel().isOnline.test {
            var online = awaitItem()
            if (online) online = awaitItem() // the initial optimistic value
            assertFalse(online)
        }
    }
}
