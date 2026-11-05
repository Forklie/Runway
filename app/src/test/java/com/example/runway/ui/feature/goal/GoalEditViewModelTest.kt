package com.example.runway.ui.feature.goal

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.DefaultAccountIds
import com.example.runway.domain.model.Goal
import com.example.runway.domain.repository.GoalRepository
import com.example.runway.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GoalEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val colombo = ZoneId.of("Asia/Colombo")
    private val now = Instant.parse("2026-11-03T04:00:00Z")
    private val time = TimeProvider(Clock.fixed(now, colombo))
    private val goals = mockk<GoalRepository>()
    private val saved = slot<Goal>()

    private val existing = Goal(
        id = Goal.MAIN_ID,
        name = "MacBook Pro M4",
        targetMinor = 49_000_000,
        startDate = Instant.parse("2026-10-25T06:30:00Z"),
        deadline = LocalDate.of(2027, 10, 25).atStartOfDay(colombo).toInstant(),
        fundAccountId = "goal-fund",
        active = true,
    )

    @Before
    fun setUp() {
        every { goals.observeActiveGoal() } returns flowOf(null)
        coEvery { goals.save(capture(saved)) } returns AppResult.Success(Unit)
    }

    private fun viewModel() = GoalEditViewModel(goals, time, SavedStateHandle())

    @Test
    fun `a new goal lands 12 months out, in the goal fund, at the fixed id (BR-70)`() = runTest {
        val vm = viewModel()
        vm.onNameChange("  MacBook Pro M4 ")
        vm.onTargetChange("490000")
        vm.onSave()
        vm.done.first()

        assertEquals(Goal.MAIN_ID, saved.captured.id)
        assertEquals("MacBook Pro M4", saved.captured.name)
        assertEquals(49_000_000L, saved.captured.targetMinor)
        assertEquals(LocalDate.of(2027, 11, 3).atStartOfDay(colombo).toInstant(), saved.captured.deadline)
        assertEquals(DefaultAccountIds.GOAL_FUND, saved.captured.fundAccountId)
        assertEquals(now, saved.captured.startDate)
    }

    @Test
    fun `a bad target or a blank name is caught before saving, keeping the input (BR-07)`() {
        val vm = viewModel()
        vm.onTargetChange("12.345")
        vm.onSave()

        assertTrue(vm.state.value.nameError)
        assertTrue(vm.state.value.targetError)
        assertEquals("12.345", vm.state.value.target)
        coVerify(exactly = 0) { goals.save(any()) }
    }

    @Test
    fun `editing keeps the start date and deadline unless a new length is chosen`() = runTest {
        every { goals.observeActiveGoal() } returns flowOf(existing)
        val vm = viewModel()

        assertFalse(vm.state.value.isNew)
        assertEquals("490000", vm.state.value.target)
        assertNull(vm.state.value.months)

        vm.onTargetChange("520000")
        vm.onSave()
        vm.done.first()
        assertEquals(existing.startDate, saved.captured.startDate)
        assertEquals(existing.deadline, saved.captured.deadline)
        assertEquals(52_000_000L, saved.captured.targetMinor)
    }

    @Test
    fun `choosing 18 months moves the deadline from today`() {
        val vm = viewModel()
        vm.onMonthsChange(18)
        assertEquals(LocalDate.of(2028, 5, 3), vm.state.value.deadline)
    }
}
