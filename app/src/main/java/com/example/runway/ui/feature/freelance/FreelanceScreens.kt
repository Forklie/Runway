package com.example.runway.ui.feature.freelance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.money.formatLkr
import com.example.runway.core.result.AppError
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.FreelanceProject
import com.example.runway.domain.model.Milestone
import com.example.runway.domain.model.MilestoneStatus
import com.example.runway.ui.components.AmountField
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.CardTitle
import com.example.runway.ui.components.CardTone
import com.example.runway.ui.components.CategoryChip
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.LoadingState
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayCard
import com.example.runway.ui.components.RunwayDatePicker
import com.example.runway.ui.components.RunwaySheet
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.SmallTopBar
import com.example.runway.ui.components.StatusTag
import com.example.runway.ui.components.TagTone
import com.example.runway.ui.components.dayMonthLabel
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.feature.home.nothingYetLockup
import com.example.runway.ui.state.UiState
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/* ---------- Project list ---------- */

@Composable
fun FreelanceRoute(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: FreelanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    FreelanceScreen(state, error, onBack, onOpen, viewModel::retry, viewModel::onAddProject)
}

@Composable
fun FreelanceScreen(
    state: UiState<List<ProjectSummary>>,
    error: AppError?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    onAddProject: (String, String, String) -> Boolean,
) {
    val c = RunwayTheme.colors
    var adding by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        SmallTopBar(
            title = stringResource(R.string.freelance_title),
            onBack = onBack,
            action = {
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.freelance_add_project), tint = c.ink)
                }
            },
        )
        ErrorLine(error)
        when (state) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> Centred {
                EmptyState(
                    lockup = nothingYetLockup(),
                    title = stringResource(R.string.freelance_empty_title),
                    body = stringResource(R.string.freelance_empty_body),
                    actionLabel = stringResource(R.string.freelance_add_project),
                    onAction = { adding = true },
                )
            }
            is UiState.Error -> Centred {
                EmptyState(
                    lockup = problemLockup(),
                    title = stringResource(R.string.error_title),
                    body = stringResource(state.error.messageRes()),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )
            }
            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(horizontal = Dimens.screenMargin, vertical = Spacing.s2),
                verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                items(state.data, key = { it.project.id }) { summary -> ProjectCard(summary) { onOpen(summary.project.id) } }
            }
        }
    }
    if (adding) AddProjectSheet(onDismiss = { adding = false }, onSave = onAddProject)
}

@Composable
private fun ProjectCard(summary: ProjectSummary, onClick: () -> Unit) {
    val c = RunwayTheme.colors
    RunwayCard(Modifier.clickable(role = Role.Button, onClick = onClick)) {
        CardTitle(
            title = summary.project.title,
            trailing = if (summary.overdueCount > 0) {
                { StatusTag(stringResource(R.string.freelance_overdue), TagTone.NeedsLook) }
            } else {
                null
            },
        )
        Text(summary.project.clientName, style = RunwayType.bodySm, color = c.inkMuted)
        Text(
            stringResource(
                R.string.freelance_project_totals,
                summary.paidMinor.formatLkr(),
                summary.project.totalMinor.formatLkr(),
                summary.pendingMinor.formatLkr(),
            ),
            style = RunwayType.bodySm,
            color = c.ink,
        )
    }
}

@Composable
private fun AddProjectSheet(onDismiss: () -> Unit, onSave: (String, String, String) -> Boolean) {
    var client by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var total by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val required = stringResource(R.string.freelance_required)
    RunwaySheet(title = stringResource(R.string.freelance_add_project), onDismiss = onDismiss) {
        RunwayTextField(
            value = client,
            onValueChange = { client = it },
            label = stringResource(R.string.freelance_client),
            errorText = required.takeIf { showErrors && client.isBlank() },
        )
        RunwayTextField(
            value = title,
            onValueChange = { title = it },
            label = stringResource(R.string.freelance_project_name),
            errorText = required.takeIf { showErrors && title.isBlank() },
        )
        AmountField(
            value = total,
            onValueChange = { total = it },
            label = stringResource(R.string.freelance_total),
            errorText = stringResource(R.string.amount_error_invalid).takeIf { showErrors && MoneyParser.parseToMinor(total) == null },
        )
        RunwayButton(
            text = stringResource(R.string.freelance_save),
            onClick = { if (onSave(client, title, total)) onDismiss() else showErrors = true },
            size = ButtonSize.Large,
            fillWidth = true,
        )
    }
}

/* ---------- One project ---------- */

@Composable
fun ProjectRoute(onBack: () -> Unit, viewModel: ProjectViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    ProjectScreen(state, error, onBack, viewModel::onAddMilestone, viewModel::onInvoice, viewModel::onMarkPaid)
}

@Composable
fun ProjectScreen(
    state: UiState<ProjectDetail>,
    error: AppError?,
    onBack: () -> Unit,
    onAddMilestone: (String, String) -> Boolean,
    onInvoice: (Milestone, LocalDate) -> Unit,
    onMarkPaid: (Milestone, String, String, String?) -> Boolean,
) {
    val c = RunwayTheme.colors
    var adding by rememberSaveable { mutableStateOf(false) }
    var invoicingId by rememberSaveable { mutableStateOf<String?>(null) }
    var payingId by rememberSaveable { mutableStateOf<String?>(null) }
    val detail = (state as? UiState.Success)?.data

    Column(Modifier.fillMaxSize().background(c.bg)) {
        SmallTopBar(
            title = detail?.project?.title ?: stringResource(R.string.freelance_title),
            onBack = onBack,
            action = {
                if (detail != null) {
                    IconButton(onClick = { adding = true }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.freelance_add_milestone), tint = c.ink)
                    }
                }
            },
        )
        ErrorLine(error)
        when (state) {
            is UiState.Success -> {
                val data = state.data
                if (data.milestones.isEmpty()) {
                    Centred {
                        EmptyState(
                            lockup = nothingYetLockup(),
                            title = stringResource(R.string.freelance_no_milestones_title),
                            body = stringResource(R.string.freelance_no_milestones_body),
                            actionLabel = stringResource(R.string.freelance_add_milestone),
                            onAction = { adding = true },
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Dimens.screenMargin, vertical = Spacing.s2),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                    ) {
                        items(data.milestones, key = { it.id }) { milestone ->
                            MilestoneCard(
                                milestone = milestone,
                                overdue = milestone.id in data.overdueIds,
                                zone = data.zone,
                                onInvoice = { invoicingId = milestone.id },
                                onMarkPaid = { payingId = milestone.id },
                            )
                        }
                    }
                }
            }
            is UiState.Error -> Centred {
                EmptyState(
                    lockup = problemLockup(),
                    title = stringResource(R.string.error_title),
                    body = stringResource(state.error.messageRes()),
                    actionLabel = stringResource(R.string.ui_back),
                    onAction = onBack,
                )
            }
            else -> LoadingState()
        }
    }

    if (adding) AddMilestoneSheet(onDismiss = { adding = false }, onSave = onAddMilestone)
    detail?.milestones?.firstOrNull { it.id == invoicingId }?.let { milestone ->
        RunwayDatePicker(
            selected = detail.today.plusDays(DEFAULT_DUE_DAYS),
            onPick = {
                onInvoice(milestone, it)
                invoicingId = null
            },
            onDismiss = { invoicingId = null },
        )
    }
    detail?.milestones?.firstOrNull { it.id == payingId }?.let { milestone ->
        MarkPaidSheet(
            milestone = milestone,
            accounts = detail.accounts,
            onDismiss = { payingId = null },
            onSave = { amount, accountId -> onMarkPaid(milestone, detail.project.title, amount, accountId) },
        )
    }
}

@Composable
private fun MilestoneCard(milestone: Milestone, overdue: Boolean, zone: ZoneId, onInvoice: () -> Unit, onMarkPaid: () -> Unit) {
    val c = RunwayTheme.colors
    RunwayCard {
        CardTitle(
            title = milestone.label,
            trailing = {
                when {
                    overdue -> StatusTag(stringResource(R.string.freelance_overdue), TagTone.NeedsLook)
                    milestone.status == MilestoneStatus.PAID -> StatusTag(stringResource(R.string.freelance_paid), TagTone.Done)
                    milestone.status == MilestoneStatus.INVOICED -> StatusTag(stringResource(R.string.freelance_invoiced), TagTone.Pending)
                    else -> StatusTag(stringResource(R.string.freelance_planned), TagTone.Pending)
                }
            },
        )
        Text(milestone.amountMinor.formatLkr(), style = RunwayType.money, color = c.ink)
        milestone.dueDate?.takeIf { milestone.status == MilestoneStatus.INVOICED }?.let {
            Text(stringResource(R.string.freelance_due, it.atZone(zone).toLocalDate().dayMonthLabel()), style = RunwayType.bodySm, color = c.inkMuted)
        }
        milestone.paidDate?.let {
            Text(stringResource(R.string.freelance_paid_on, it.atZone(zone).toLocalDate().dayMonthLabel()), style = RunwayType.bodySm, color = c.inkMuted)
        }
        when (milestone.status) {
            MilestoneStatus.PLANNED -> RunwayButton(stringResource(R.string.freelance_send_invoice), onInvoice, style = ButtonStyle.Tonal)
            MilestoneStatus.INVOICED -> RunwayButton(stringResource(R.string.freelance_mark_paid), onMarkPaid)
            MilestoneStatus.PAID -> Unit
        }
    }
}

@Composable
private fun AddMilestoneSheet(onDismiss: () -> Unit, onSave: (String, String) -> Boolean) {
    var label by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    RunwaySheet(title = stringResource(R.string.freelance_add_milestone), onDismiss = onDismiss) {
        RunwayTextField(
            value = label,
            onValueChange = { label = it },
            label = stringResource(R.string.freelance_milestone_name),
            errorText = stringResource(R.string.freelance_required).takeIf { showErrors && label.isBlank() },
        )
        AmountField(
            value = amount,
            onValueChange = { amount = it },
            label = stringResource(R.string.freelance_amount),
            errorText = stringResource(R.string.amount_error_invalid).takeIf { showErrors && MoneyParser.parseToMinor(amount) == null },
        )
        RunwayButton(
            text = stringResource(R.string.freelance_save),
            onClick = { if (onSave(label, amount)) onDismiss() else showErrors = true },
            size = ButtonSize.Large,
            fillWidth = true,
        )
    }
}

/** BR-52: prompts for the account and the amount received, which may differ from what was invoiced. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkPaidSheet(milestone: Milestone, accounts: List<Account>, onDismiss: () -> Unit, onSave: (String, String?) -> Boolean) {
    val c = RunwayTheme.colors
    var amount by rememberSaveable { mutableStateOf(BigDecimal.valueOf(milestone.amountMinor, 2).stripTrailingZeros().toPlainString()) }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    RunwaySheet(title = stringResource(R.string.freelance_mark_paid), onDismiss = onDismiss) {
        AmountField(
            value = amount,
            onValueChange = { amount = it },
            label = stringResource(R.string.freelance_received),
            errorText = stringResource(R.string.amount_error_invalid).takeIf { showErrors },
        )
        Text(stringResource(R.string.freelance_paid_into), style = RunwayType.label, color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            accounts.forEach { account ->
                CategoryChip(label = account.name, selected = account.id == accountId, onClick = { accountId = account.id })
            }
        }
        RunwayButton(
            text = stringResource(R.string.freelance_save_paid),
            onClick = { if (onSave(amount, accountId)) onDismiss() else showErrors = true },
            size = ButtonSize.Large,
            fillWidth = true,
        )
    }
}

/* ---------- Home's card ---------- */

/**
 * Overdue invoices on Home (BR-53). Draws nothing when none are overdue. Opens the project, where Mark paid lives.
 * ponytail: no snooze, BR-53's second action. It needs a stored snoozedUntil on the milestone.
 */
@Composable
fun OverdueMilestonesCard(onOpenProject: (String) -> Unit, viewModel: OverdueViewModel = hiltViewModel()) {
    val overdue by viewModel.overdue.collectAsStateWithLifecycle()
    if (overdue.isEmpty()) return
    val c = RunwayTheme.colors
    RunwayCard(
        modifier = Modifier.padding(horizontal = Dimens.screenMargin).padding(top = Spacing.s3),
        tone = CardTone.Note,
    ) {
        CardTitle(
            title = stringResource(R.string.freelance_overdue_card),
            trailing = { StatusTag(stringResource(R.string.freelance_overdue), TagTone.NeedsLook) },
        )
        overdue.forEach { item ->
            Text(
                stringResource(R.string.freelance_overdue_row, item.project.clientName, item.milestone.label, item.milestone.amountMinor.formatLkr()),
                style = RunwayType.bodySm,
                color = c.ink,
            )
        }
        RunwayButton(
            text = stringResource(R.string.freelance_overdue_open),
            onClick = { onOpenProject(overdue.first().project.id) },
            style = ButtonStyle.Tonal,
            size = ButtonSize.Small,
        )
    }
}

@Composable
private fun ErrorLine(error: AppError?) {
    if (error != null) {
        Text(
            stringResource(error.messageRes()),
            style = RunwayType.bodySm,
            color = RunwayTheme.colors.danger,
            modifier = Modifier.padding(horizontal = Dimens.screenMargin),
        )
    }
}

@Composable
private fun Centred(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private const val DEFAULT_DUE_DAYS = 30L

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun FreelancePreview() {
    val project = FreelanceProject("p", "Lanka Tea", "Website", 15_000_000, Instant.EPOCH, false)
    RunwayTheme {
        FreelanceScreen(UiState.Success(listOf(ProjectSummary(project, 5_000_000, 5_000_000, 1))), null, {}, {}, {}, { _, _, _ -> true })
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ProjectPreview() {
    val project = FreelanceProject("p", "Lanka Tea", "Website", 15_000_000, Instant.EPOCH, false)
    val milestones = listOf(
        Milestone("1", "p", "Design", 5_000_000, MilestoneStatus.PAID, null, null, Instant.EPOCH, "t"),
        Milestone("2", "p", "Build", 5_000_000, MilestoneStatus.INVOICED, null, Instant.EPOCH, null, null),
        Milestone("3", "p", "Launch", 5_000_000, MilestoneStatus.PLANNED, null, null, null, null),
    )
    RunwayTheme {
        ProjectScreen(
            UiState.Success(ProjectDetail(project, milestones, emptyList(), LocalDate.of(2026, 11, 4), setOf("2"), ZoneId.of("Asia/Colombo"))),
            null, {}, { _, _ -> true }, { _, _ -> }, { _, _, _, _ -> true },
        )
    }
}
