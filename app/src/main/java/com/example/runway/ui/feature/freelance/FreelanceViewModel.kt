package com.example.runway.ui.feature.freelance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.event.AppEvent
import com.example.runway.core.event.AppEvents
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.FreelanceProject
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.MilestoneStatus
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.FreelanceRepository
import com.example.runway.domain.usecase.MarkMilestonePaid
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row of the list. Paid and pending are worked out from the milestones, never stored. */
data class ProjectSummary(val project: FreelanceProject, val paidMinor: Long, val pendingMinor: Long, val overdueCount: Int)

data class ProjectDetail(
    val project: FreelanceProject,
    val milestones: List<Milestone>,
    val accounts: List<Account>,
    val today: LocalDate,
    val zone: ZoneId,
)

data class OverdueItem(val milestone: Milestone, val project: FreelanceProject)

/** A failed write reaches the person through the shell's snackbar, like one the server refused. */
private fun report(result: AppResult<Unit>) {
    if (result is AppResult.Failure) AppEvents.emit(AppEvent.WriteRejected)
}

private fun List<Milestone>.sumOf(status: MilestoneStatus) = filter { it.status == status }.sumOf { it.amountMinor }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FreelanceViewModel @Inject constructor(
    private val freelance: FreelanceRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<UiState<List<ProjectSummary>>> = retryTick
        .flatMapLatest {
            combine(freelance.observeProjects(), freelance.observeMilestones()) { projects, milestones ->
                val summaries = projects.map { project ->
                    val own = milestones.filter { it.projectId == project.id }
                    ProjectSummary(
                        project = project,
                        paidMinor = own.sumOf(MilestoneStatus.PAID),
                        pendingMinor = own.sumOf(MilestoneStatus.INVOICED),
                        overdueCount = own.count { it.isOverdue(time.today(), time.zone) },
                    )
                }
                if (summaries.isEmpty()) UiState.Empty else UiState.Success(summaries)
            }
                .onStart<UiState<List<ProjectSummary>>> { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() = retryTick.update { it + 1 }

    /** False leaves the sheet open with the input kept; the sheet shows the inline errors. */
    fun onAddProject(client: String, title: String, total: String): Boolean {
        val amount = MoneyParser.parseToMinor(total)
        if (client.isBlank() || title.isBlank() || amount == null) return false
        val project = FreelanceProject(UUID.randomUUID().toString(), client.trim(), title.trim(), amount, time.now(), false)
        viewModelScope.launch { report(freelance.saveProject(project)) }
        return true
    }
}

/** One project: add milestones, invoice them, mark them paid (BR-50, BR-52). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val freelance: FreelanceRepository,
    accounts: AccountRepository,
    private val markMilestonePaid: MarkMilestonePaid,
    private val time: TimeProvider,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val projectId: String = savedState[KEY_PROJECT_ID] ?: ""

    val uiState: StateFlow<UiState<ProjectDetail>> = combine(
        freelance.observeProjects(),
        freelance.observeMilestones(),
        accounts.observeAccounts(),
    ) { projects, milestones, allAccounts ->
        val project = projects.firstOrNull { it.id == projectId }
        if (project == null) {
            UiState.Error(AppError.NotFound)
        } else {
            val own = milestones
                .filter { it.projectId == projectId }
                .sortedWith(compareBy({ it.dueDate == null }, { it.dueDate }, { it.label }))
            UiState.Success(
                ProjectDetail(
                    project = project,
                    milestones = own,
                    accounts = allAccounts.filter { it.kind != AccountKind.GOAL_FUND },
                    today = time.today(),
                    zone = time.zone,
                ),
            )
        }
    }
        .onStart<UiState<ProjectDetail>> { emit(UiState.Loading) }
        .catch { emit(UiState.Error(it.toAppError())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun onAddMilestone(label: String, amount: String): Boolean {
        val minor = MoneyParser.parseToMinor(amount)
        if (label.isBlank() || minor == null) return false
        write {
            freelance.saveMilestone(
                Milestone(UUID.randomUUID().toString(), projectId, label.trim(), minor, MilestoneStatus.PLANNED, null, null, null, null),
            )
        }
        return true
    }

    /** PLANNED to INVOICED. A due date in the past is allowed: the invoice may have gone out earlier. */
    fun onInvoice(milestone: Milestone, due: LocalDate) = write {
        freelance.saveMilestone(
            milestone.copy(
                status = MilestoneStatus.INVOICED,
                invoiceDate = time.now(),
                dueDate = due.atStartOfDay(time.zone).toInstant(),
            ),
        )
    }

    fun onMarkPaid(milestone: Milestone, projectTitle: String, amount: String, accountId: String?): Boolean {
        val minor = MoneyParser.parseToMinor(amount)
        if (minor == null || accountId == null) return false
        write { markMilestonePaid(milestone, projectTitle, minor, accountId) }
        return true
    }

    private fun write(block: suspend () -> AppResult<Unit>) {
        viewModelScope.launch { report(block()) }
    }

    private companion object {
        const val KEY_PROJECT_ID = "projectId" // the route's argument name (ProjectDest.projectId)
    }
}

/** Feeds Home's card (BR-53). A failure just hides the card: Home has its own error state. */
@HiltViewModel
class OverdueViewModel @Inject constructor(
    freelance: FreelanceRepository,
    time: TimeProvider,
) : ViewModel() {
    val overdue: StateFlow<List<OverdueItem>> = combine(freelance.observeProjects(), freelance.observeMilestones()) { projects, milestones ->
        val byId = projects.associateBy { it.id }
        milestones
            .filter { it.isOverdue(time.today(), time.zone) }
            .mapNotNull { milestone -> byId[milestone.projectId]?.let { OverdueItem(milestone, it) } }
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
