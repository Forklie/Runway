package com.example.runway.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.GoalSettings
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.repository.GoalSettingsRepository
import com.example.runway.domain.repository.UserProfileRepository
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What Settings shows, all read live (FR-24). */
data class SettingsContent(
    val displayName: String,
    val email: String,
    val goal: GoalSettings?,
    val accounts: List<Account>,
    val defaultAccountId: String?,
    val pyfPercent: Double,
)

/** The goal edit sheet. [months] null means "keep the current deadline". */
data class GoalEditForm(
    val name: String,
    val target: String,
    val months: Int? = null,
    val nameError: Boolean = false,
    val targetError: Boolean = false,
)

/**
 * Unsaved edits. A null draft means "show the saved value", so there is nothing to keep in sync
 * when the profile changes under us.
 */
data class SettingsForm(
    val nameDraft: String? = null,
    val pyfDraft: String? = null,
    val nameError: Boolean = false,
    val pyfError: Boolean = false,
    /** Non-null while the goal sheet is open. */
    val goalEdit: GoalEditForm? = null,
    /** A save that could not even be queued. Shown as a banner. */
    val actionError: AppError? = null,
)

/**
 * Settings: profile name, goal, default account, PYF percentage, sign out (FR-24).
 * Content is read live through three listeners; edits are validated here and written through the
 * repositories. Signing out changes the session and RunwayShell sends the user to Login.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val goalRepository: GoalSettingsRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val time: TimeProvider,
) : ViewModel() {

    /** Loading until the profile arrives. There is no Empty case: a signed-in user always has a profile. */
    val uiState: StateFlow<UiState<SettingsContent>> = combine(
        profileRepository.observeProfile(),
        goalRepository.observeGoal(),
        accountRepository.observeAccounts(),
    ) { profile, goal, accounts ->
        val state: UiState<SettingsContent> =
            if (profile == null) {
                UiState.Loading
            } else {
                UiState.Success(
                    SettingsContent(
                        displayName = profile.displayName,
                        email = profile.email,
                        goal = goal,
                        accounts = accounts,
                        defaultAccountId = profile.defaultAccountId,
                        pyfPercent = profile.pyfPercent,
                    ),
                )
            }
        state
    }
        .catch { emit(UiState.Error(it.toAppError())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _form = MutableStateFlow(SettingsForm())
    val form: StateFlow<SettingsForm> = _form.asStateFlow()

    // ---- Profile name ----

    fun onNameChange(value: String) = _form.update { it.copy(nameDraft = value, nameError = false, actionError = null) }

    fun onSaveName() {
        val name = _form.value.nameDraft?.trim() ?: return
        if (name.isEmpty()) {
            _form.update { it.copy(nameError = true) }
            return
        }
        save(onSuccess = { _form.update { it.copy(nameDraft = null) } }) { profileRepository.setDisplayName(name) }
    }

    // ---- Pay Yourself First percentage ----

    fun onPyfChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '.' }.take(5)
        _form.update { it.copy(pyfDraft = cleaned, pyfError = false, actionError = null) }
    }

    fun onSavePyf() {
        val draft = _form.value.pyfDraft ?: return
        val percent = draft.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }
        if (percent == null) {
            _form.update { it.copy(pyfError = true) }
            return
        }
        save(onSuccess = { _form.update { it.copy(pyfDraft = null) } }) { profileRepository.setPyfPercent(percent) }
    }

    // ---- Default account ----

    fun onDefaultAccountSelected(accountId: String) {
        save { profileRepository.setDefaultAccount(accountId) }
    }

    // ---- Goal ----

    fun onEditGoal() {
        val goal = (uiState.value as? UiState.Success)?.data?.goal ?: return
        _form.update {
            it.copy(goalEdit = GoalEditForm(name = goal.name, target = goal.targetMinor.toPlainAmount()), actionError = null)
        }
    }

    fun onGoalNameChange(value: String) = editGoal { it.copy(name = value, nameError = false) }
    fun onGoalTargetChange(value: String) = editGoal { it.copy(target = value, targetError = false) }

    /** Tapping the chosen deadline again goes back to "keep the current deadline". */
    fun onGoalMonthsChange(months: Int) = editGoal { it.copy(months = if (it.months == months) null else months) }

    fun onDismissGoal() = _form.update { it.copy(goalEdit = null) }

    fun onSaveGoal() {
        val edit = _form.value.goalEdit ?: return
        val name = edit.name.trim()
        val target = MoneyParser.parseToMinor(edit.target)
        if (name.isEmpty() || target == null) {
            editGoal { it.copy(nameError = name.isEmpty(), targetError = target == null) }
            return
        }
        val deadline = edit.months?.let { time.now().atZone(time.zone).plusMonths(it.toLong()).toInstant() }
        save(onSuccess = { _form.update { it.copy(goalEdit = null) } }) {
            goalRepository.updateGoal(name, target, deadline)
        }
    }

    // ---- Sign out ----

    /** No navigation here: the auth state changes and the shell sends the user to Login. */
    fun onSignOut() = authRepository.signOut()

    // ---- Plumbing ----

    private fun editGoal(transform: (GoalEditForm) -> GoalEditForm) =
        _form.update { form -> form.goalEdit?.let { form.copy(goalEdit = transform(it)) } ?: form }

    private fun save(onSuccess: () -> Unit = {}, call: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            when (val result = call()) {
                is AppResult.Success -> onSuccess()
                is AppResult.Failure -> _form.update { it.copy(actionError = result.error) }
            }
        }
    }
}

/** 49_000_000 -> "490000", 125_050 -> "1250.5": the plain text a person would type back. */
internal fun Long.toPlainAmount(): String = BigDecimal.valueOf(this, 2).stripTrailingZeros().toPlainString()
