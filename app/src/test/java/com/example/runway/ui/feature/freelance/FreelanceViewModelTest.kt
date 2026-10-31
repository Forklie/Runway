package com.example.runway.ui.feature.freelance

import androidx.lifecycle.SavedStateHandle
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.DefaultIncomeSourceIds
import com.example.runway.domain.model.FreelanceProject
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.MilestoneStatus
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.FreelanceRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.AddTransaction
import com.example.runway.domain.usecase.MarkMilestonePaid
import com.example.runway.testutil.Fixtures.account
import com.example.runway.testutil.Fixtures.at
import com.example.runway.testutil.Fixtures.colombo
import com.example.runway.testutil.MainDispatcherRule
import com.example.runway.ui.state.UiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreelanceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<FreelanceRepository>()
    private val accounts = mockk<AccountRepository>()
    private val time = TimeProvider(Clock.fixed(Instant.parse("2026-11-10T04:00:00Z"), colombo))
    private val today = LocalDate.of(2026, 11, 10)

    private val project = FreelanceProject("p", "Lanka Tea", "Website", 15_000_000, at("2026-10-01"))
    private val paid = milestone("paid", MilestoneStatus.PAID)
    private val late = milestone("late", MilestoneStatus.INVOICED, due = at("2026-11-09"))
    private val dueToday = milestone("today", MilestoneStatus.INVOICED, due = at("2026-11-10"))
    private val planned = milestone("planned", MilestoneStatus.PLANNED, due = at("2026-01-01"))

    private val savedMilestones = mutableListOf<Milestone>()
    private val paidMilestones = mutableListOf<Milestone>()
    private val incomes = mutableListOf<Transaction>()

    private fun milestone(id: String, status: MilestoneStatus, due: Instant? = null) =
        Milestone(id, "p", id, 5_000_000, status, null, due, null, null)

    @Before
    fun setUp() {
        every { repo.observeProjects() } returns flowOf(listOf(project))
        every { repo.observeMilestones() } returns flowOf(listOf(paid, late, dueToday, planned))
        every { accounts.observeAccounts() } returns flowOf(listOf(account("fund", AccountKind.GOAL_FUND), account("bank")))
        coEvery { repo.saveMilestone(capture(savedMilestones)) } returns AppResult.Success(Unit)
        coEvery { repo.saveProject(any()) } returns AppResult.Success(Unit)
        coEvery { repo.markPaid(capture(paidMilestones), capture(incomes)) } returns AppResult.Success(Unit)
    }

    private fun TestScope.projectViewModel() = ProjectViewModel(
        freelance = repo,
        accounts = accounts,
        markMilestonePaid = MarkMilestonePaid(AddTransaction(mockk<TransactionRepository>(), time), repo, time),
        time = time,
        savedState = SavedStateHandle(mapOf("projectId" to "p")),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    @Test
    fun `overdue is derived, only for invoiced items due before today (BR-51, AT-13)`() {
        assertTrue(late.isOverdue(today, colombo))
        assertFalse(dueToday.isOverdue(today, colombo))
        assertFalse(planned.isOverdue(today, colombo))
        assertFalse(paid.copy(dueDate = at("2026-01-01")).isOverdue(today, colombo))
    }

    @Test
    fun `Home's card lists only the overdue milestones with their project (BR-53)`() = runTest {
        val vm = OverdueViewModel(repo, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.overdue.collect {} }
        assertEquals(listOf("late"), vm.overdue.value.map { it.milestone.id })
        assertEquals("Lanka Tea", vm.overdue.value.single().project.clientName)
    }

    @Test
    fun `the list works out paid, invoiced and overdue from the milestones`() = runTest {
        val vm = FreelanceViewModel(repo, time)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val summary = (vm.uiState.value as UiState.Success).data.single()
        assertEquals(5_000_000L, summary.paidMinor)
        assertEquals(10_000_000L, summary.pendingMinor)
        assertEquals(1, summary.overdueCount)
    }

    @Test
    fun `marking paid writes the milestone and a linked income together (BR-52, AT-14)`() = runTest {
        val vm = projectViewModel()
        assertTrue(vm.onMarkPaid(late, "Website", "45,000", "bank"))

        val income = incomes.single()
        val milestone = paidMilestones.single()
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals(4_500_000L, income.amountLkrMinor)
        assertEquals("bank", income.accountId)
        assertEquals(DefaultIncomeSourceIds.FREELANCE, income.incomeSourceId)
        assertEquals(MilestoneStatus.PAID, milestone.status)
        assertEquals(income.id, milestone.linkedTxId)
        assertEquals(income.date, milestone.paidDate)
    }

    @Test
    fun `a retry of mark paid uses the same transaction id (BR-19)`() = runTest {
        val vm = projectViewModel()
        vm.onMarkPaid(late, "Website", "100", "bank")
        vm.onMarkPaid(late, "Website", "100", "bank")
        assertEquals(1, incomes.map { it.id }.toSet().size)
    }

    @Test
    fun `invalid input keeps the sheet open and writes nothing`() = runTest {
        val vm = projectViewModel()
        assertFalse(vm.onMarkPaid(late, "Website", "0", "bank"))
        assertFalse(vm.onMarkPaid(late, "Website", "100", null))
        assertFalse(vm.onAddMilestone(" ", "100"))
        assertFalse(FreelanceViewModel(repo, time).onAddProject("", "Website", "100"))
        assertTrue(incomes.isEmpty())
        assertTrue(savedMilestones.isEmpty())
    }

    @Test
    fun `invoicing stores INVOICED with the due date, a past date is allowed`() = runTest {
        val vm = projectViewModel()
        vm.onInvoice(planned, LocalDate.of(2026, 11, 1))
        val saved = savedMilestones.single()
        assertEquals(MilestoneStatus.INVOICED, saved.status)
        assertEquals(time.now(), saved.invoiceDate)
        assertTrue(saved.isOverdue(today, colombo))
    }

    @Test
    fun `the goal fund is not offered as a payment account, an unknown project is not found`() = runTest {
        val detail = (projectViewModel().uiState.value as UiState.Success).data
        assertEquals(listOf("bank"), detail.accounts.map { it.id })

        every { repo.observeProjects() } returns flowOf(emptyList())
        assertTrue(projectViewModel().uiState.value is UiState.Error)
    }
}
