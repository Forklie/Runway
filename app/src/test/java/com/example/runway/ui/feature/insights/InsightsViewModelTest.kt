package com.example.runway.ui.feature.insights

import app.cash.turbine.test
import com.example.runway.domain.usecase.ObserveInsights
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InsightsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeInsights = mockk<ObserveInsights>()
    private val insights = previewInsights()

    @Test
    fun `live insights are shown as Success`() = runTest {
        every { observeInsights() } returns flowOf(insights)
        InsightsViewModel(observeInsights).uiState.test {
            var item = awaitItem()
            if (item == UiState.Loading) item = awaitItem()
            assertEquals(UiState.Success(insights), item)
        }
    }

    @Test
    fun `a failed listener is an Error, and Try again recovers (Z9)`() = runTest {
        every { observeInsights() } returnsMany listOf(flow { throw IllegalStateException("No signed-in user") }, flowOf(insights))
        val vm = InsightsViewModel(observeInsights)
        vm.uiState.test {
            var item = awaitItem()
            if (item == UiState.Loading) item = awaitItem()
            assertTrue(item is UiState.Error)
            vm.retry()
            item = awaitItem()
            if (item == UiState.Loading) item = awaitItem()
            assertEquals(UiState.Success(insights), item)
        }
    }
}
