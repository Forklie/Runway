package com.example.runway.ui.feature.subscriptions

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.RecurringKind
import com.example.runway.domain.model.UserProfile
import com.example.runway.domain.repository.RecurringRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.domain.usecase.ComputeSubscriptionCost
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubscriptionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemsFlow = MutableStateFlow(listOf<RecurringItem>())
    private val profileFlow = MutableStateFlow<UserProfile?>(profile())
    private val recurring = mockk<RecurringRepository>()
    private val profiles = mockk<UserProfileRepository>()

    @Before
    fun setUp() {
        every { recurring.observeSubscriptions() } returns itemsFlow
        every { profiles.observeProfile() } returns profileFlow
        coEvery { recurring.setActive(any(), any()) } returns AppResult.Success(Unit)
    }

    private fun viewModel() = SubscriptionsViewModel(recurring, profiles, ComputeSubscriptionCost())

    private fun profile(rates: Map<String, Double> = emptyMap()) = UserProfile(
        uid = "u1", displayName = "Kavindu", email = "k@example.com", lastFxRates = rates, createdAt = Instant.EPOCH,
    )

    private fun sub(
        id: String,
        name: String,
        amount: Long,
        active: Boolean = true,
        currency: CurrencyCode = CurrencyCode.LKR,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    ) = RecurringItem(id, RecurringKind.SUBSCRIPTION, name, amount, currency, frequency, Instant.EPOCH, "subscriptions", "bank-main", active)

    private suspend fun ReceiveTurbine<UiState<SubscriptionsContent>>.awaitSettled(): UiState<SubscriptionsContent> {
        var item = awaitItem()
        while (item == UiState.Loading) item = awaitItem()
        return item
    }

    private suspend fun ReceiveTurbine<UiState<SubscriptionsContent>>.awaitContent(): SubscriptionsContent =
        (awaitSettled() as UiState.Success).data

    @Test
    fun `no subscriptions is the empty state`() = runTest {
        viewModel().uiState.test {
            assertEquals(UiState.Empty, awaitSettled())
        }
    }

    @Test
    fun `shows monthly and yearly cost per subscription and in total (BR-92)`() = runTest {
        itemsFlow.value = listOf(sub("a", "Streaming", 150_000), sub("b", "Music", 50_000))
        viewModel().uiState.test {
            val content = awaitContent()
            assertEquals(2, content.active.size)
            assertEquals(150_000L, content.active[0].monthlyLkrMinor)
            assertEquals(1_800_000L, content.active[0].yearlyLkrMinor)   // x 12
            assertEquals(200_000L, content.monthlyTotalMinor)            // LKR 2,000 a month
            assertEquals(2_400_000L, content.yearlyTotalMinor)           // LKR 24,000 a year
            assertEquals(0L, content.yearlySavedMinor)
        }
    }

    @Test
    fun `answering no moves the item to cancelled and shows the annual saving (BR-93)`() = runTest {
        itemsFlow.value = listOf(sub("a", "Streaming", 150_000), sub("b", "Music", 50_000))
        val vm = viewModel()
        vm.uiState.test {
            awaitContent()
            vm.onCancel("a")
            coVerify(exactly = 1) { recurring.setActive("a", false) }

            // The repository's listener re-emits with the item inactive.
            itemsFlow.value = listOf(sub("a", "Streaming", 150_000, active = false), sub("b", "Music", 50_000))
            var content = awaitContent()
            while (content.cancelled.isEmpty()) content = awaitContent()

            assertEquals(listOf("b"), content.active.map { it.id })
            assertEquals(listOf("a"), content.cancelled.map { it.id })
            assertEquals(1_800_000L, content.yearlySavedMinor)  // LKR 18,000 a year saved
            assertEquals(50_000L, content.monthlyTotalMinor)    // cancelled leaves the total
        }
    }

    @Test
    fun `answering yes writes nothing and marks the row as kept`() = runTest {
        itemsFlow.value = listOf(sub("a", "Streaming", 150_000))
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(false, awaitContent().active.single().kept)
            vm.onKeep("a")
            var content = awaitContent()
            while (!content.active.single().kept) content = awaitContent()
            assertTrue(content.active.single().kept)
        }
        coVerify(exactly = 0) { recurring.setActive(any(), any()) }
    }

    @Test
    fun `undo sets the subscription active again`() {
        viewModel().onRestore("a")
        coVerify(exactly = 1) { recurring.setActive("a", true) }
    }

    @Test
    fun `a yearly subscription is shown as a twelfth a month`() = runTest {
        itemsFlow.value = listOf(sub("a", "Cloud storage", 1_200_000, frequency = RecurringFrequency.YEARLY))
        viewModel().uiState.test {
            val row = awaitContent().active.single()
            assertEquals(100_000L, row.monthlyLkrMinor)
            assertEquals(1_200_000L, row.yearlyLkrMinor)
        }
    }

    @Test
    fun `a USD subscription uses the profile rate, and is left out of totals without one`() = runTest {
        itemsFlow.value = listOf(sub("a", "Design tool", 1_000, currency = CurrencyCode.USD), sub("b", "Music", 50_000))
        viewModel().uiState.test {
            val noRate = awaitContent()
            assertNull(noRate.active[0].monthlyLkrMinor)
            assertEquals(50_000L, noRate.monthlyTotalMinor)

            profileFlow.value = profile(mapOf("USD" to 305.0))
            var withRate = awaitContent()
            while (withRate.active[0].monthlyLkrMinor == null) withRate = awaitContent()
            assertEquals(305_000L, withRate.active[0].monthlyLkrMinor)
            assertEquals(355_000L, withRate.monthlyTotalMinor)
        }
    }

    @Test
    fun `a failed change shows an error that can be dismissed`() {
        coEvery { recurring.setActive(any(), any()) } returns AppResult.Failure(AppError.Unknown())
        val vm = viewModel()
        vm.onCancel("a")
        assertEquals(AppError.Unknown(), vm.actionError.value)
        vm.onDismissError()
        assertNull(vm.actionError.value)
    }

    @Test
    fun `a failing listener becomes an error state`() = runTest {
        every { recurring.observeSubscriptions() } returns flow { throw IllegalStateException("No signed-in user") }
        viewModel().uiState.test {
            assertTrue(awaitSettled() is UiState.Error)
        }
    }
}
