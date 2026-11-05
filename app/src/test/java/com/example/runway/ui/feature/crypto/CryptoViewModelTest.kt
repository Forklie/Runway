package com.example.runway.ui.feature.crypto

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import com.example.runway.domain.repository.CryptoRepository
import com.example.runway.domain.usecase.ComputeCryptoPosition
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CryptoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-11-03T04:00:00Z")
    private val time = TimeProvider(Clock.fixed(now, ZoneId.of("Asia/Colombo")))
    private val repository = mockk<CryptoRepository>()
    private val saved = slot<CryptoEntry>()

    @Before
    fun setUp() {
        every { repository.observeEntries() } returns flowOf(emptyList())
        coEvery { repository.add(capture(saved)) } returns AppResult.Success(Unit)
        coEvery { repository.remove(any()) } returns AppResult.Success(Unit)
    }

    private fun viewModel() = CryptoViewModel(repository, ComputeCryptoPosition(), time, SavedStateHandle())

    @Test
    fun `no entries yet is the Empty state`() = runTest {
        viewModel().uiState.test {
            var item = awaitItem()
            if (item == UiState.Loading) item = awaitItem()
            assertEquals(UiState.Empty, item)
        }
    }

    @Test
    fun `entries come with their net position (BR-61)`() = runTest {
        val entries = listOf(
            CryptoEntry("d", CryptoEntryType.DEPOSIT, "USDT", 10_000_000, now, ""),
            CryptoEntry("s", CryptoEntryType.HOLDING_SNAPSHOT, "USDT", 6_200_000, now, ""),
        )
        every { repository.observeEntries() } returns flowOf(entries)
        viewModel().uiState.test {
            var item = awaitItem()
            if (item == UiState.Loading) item = awaitItem()
            assertEquals(-3_800_000L, (item as UiState.Success).data.position.netMinor)
        }
    }

    @Test
    fun `a bad amount stays in the sheet with an inline error (AT-03)`() {
        val vm = viewModel()
        vm.onOpenAdd()
        vm.onAmountChange("0")
        vm.onSave()

        assertTrue(vm.form.value.amountError)
        assertTrue(vm.form.value.open)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `saving writes one entry in LKR and offers undo`() = runTest {
        val vm = viewModel()
        vm.onOpenAdd()
        vm.onTypeChange(CryptoEntryType.WITHDRAWAL)
        vm.onAssetChange(" USDT ")
        vm.onAmountChange("50000")
        vm.onSave()
        vm.onSave() // a second tap while the sheet closes

        val event = vm.events.first() as CryptoEvent.Added
        coVerify(exactly = 1) { repository.add(any()) }
        assertEquals(CryptoEntryType.WITHDRAWAL, saved.captured.type)
        assertEquals("USDT", saved.captured.asset)
        assertEquals(5_000_000L, saved.captured.amountLkrMinor)
        assertEquals(now, saved.captured.date)
        assertFalse(vm.form.value.open)

        vm.onUndo(event.id)
        coVerify { repository.remove(saved.captured.id) }
    }
}
