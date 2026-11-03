package com.example.runway.ui.feature.goal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.money.MoneyParser
import com.example.runway.core.result.AppError
import com.example.runway.core.result.AppResult
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.DefaultAccountIds
import com.example.runway.domain.model.Goal
import com.example.runway.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalFormState(
    val today: LocalDate,
    val name: String = "",
    val target: String = "",
    /** Months from today to the deadline. Null keeps the current goal's date. */
    val months: Int? = DEFAULT_MONTHS,
    val currentDeadline: LocalDate? = null,
    val isNew: Boolean = true,
    val nameError: Boolean = false,
    val targetError: Boolean = false,
    val formError: AppError? = null,
    val saving: Boolean = false,
) {
    val deadline: LocalDate get() = months?.let { today.plusMonths(it.toLong()) } ?: currentDeadline ?: today.plusMonths(DEFAULT_MONTHS.toLong())

    companion object {
        const val DEFAULT_MONTHS = 12
        val MONTH_CHOICES = listOf(6, 12, 18, 24)
    }
}

/**
 * Set or edit the one goal (BR-70): name, target, by when. Writes Goal.MAIN_ID, so onboarding and this
 * form never create two goals. "Saved" is not entered here: it is the goal fund's balance (BR-71).
 * Typed input survives process death through SavedStateHandle.
 */
@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goals: GoalRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        GoalFormState(
            today = time.today(),
            name = savedState[KEY_NAME] ?: "",
            target = savedState[KEY_TARGET] ?: "",
            months = if (savedState.contains(KEY_MONTHS)) savedState[KEY_MONTHS] else GoalFormState.DEFAULT_MONTHS,
        ),
    )
    val state: StateFlow<GoalFormState> = _state.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done: Flow<Unit> = _done.receiveAsFlow()

    private var existing: Goal? = null

    init {
        viewModelScope.launch {
            val goal = goals.observeActiveGoal().catch { emit(null) }.first()
            existing = goal
            if (goal == null) return@launch
            val typedBefore = savedState.contains(KEY_NAME) // restored after process death: keep the input
            _state.update {
                it.copy(
                    name = if (typedBefore) it.name else goal.name,
                    target = if (typedBefore) it.target else goal.targetMinor.toInput(),
                    months = if (typedBefore) it.months else null,
                    currentDeadline = goal.deadline.atZone(time.zone).toLocalDate(),
                    isNew = false,
                )
            }
        }
    }

    fun onNameChange(value: String) {
        savedState[KEY_NAME] = value
        _state.update { it.copy(name = value, nameError = false) }
    }

    fun onTargetChange(value: String) {
        savedState[KEY_TARGET] = value
        _state.update { it.copy(target = value, targetError = false) }
    }

    fun onMonthsChange(months: Int) {
        savedState[KEY_MONTHS] = months
        _state.update { it.copy(months = months) }
    }

    /** Validates on save, never per keystroke, and keeps what was typed (Doc 1 section 9.5). */
    fun onSave() {
        val form = _state.value
        if (form.saving) return
        val target = MoneyParser.parseToMinor(form.target)
        if (form.name.isBlank() || target == null) {
            _state.update { it.copy(nameError = form.name.isBlank(), targetError = target == null) }
            return
        }
        val goal = Goal(
            id = Goal.MAIN_ID,
            name = form.name.trim(),
            targetMinor = target,
            startDate = existing?.startDate ?: time.now(),
            deadline = form.deadline.atStartOfDay(time.zone).toInstant(),
            fundAccountId = existing?.fundAccountId ?: DefaultAccountIds.GOAL_FUND,
            active = true,
        )
        _state.update { it.copy(saving = true, formError = null) }
        viewModelScope.launch {
            when (val result = goals.save(goal)) {
                is AppResult.Success -> _done.send(Unit)
                is AppResult.Failure -> _state.update { it.copy(saving = false, formError = result.error) }
            }
        }
    }

    private fun Long.toInput(): String = BigDecimal.valueOf(this, 2).stripTrailingZeros().toPlainString()

    private companion object {
        const val KEY_NAME = "goal_form_name"
        const val KEY_TARGET = "goal_form_target"
        const val KEY_MONTHS = "goal_form_months"
    }
}
