package com.example.runway.ui.feature.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.result.toAppError
import com.example.runway.core.time.MonthKey
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.spentMinor
import com.example.runway.ui.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
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

data class ActivityDay(val date: LocalDate, val spentMinor: Long, val transactions: List<Transaction>)

data class ActivityContent(
    val days: List<ActivityDay>,
    val filters: List<Category>, // only the categories used this month
    val selectedCategoryId: String?,
    val categoriesById: Map<String, Category>,
    val accountsById: Map<String, Account>,
    val sourcesById: Map<String, IncomeSource>,
    val zone: ZoneId,
)

/** One month of history (F-08). The category filter runs on the client: a month is a small list. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val accounts: AccountRepository,
    private val sources: IncomeSourceRepository,
    private val time: TimeProvider,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val retryTick = MutableStateFlow(0)

    val month: StateFlow<String> = savedState.getStateFlow(KEY_MONTH, time.currentMonthKey())
    private val selectedCategory = savedState.getStateFlow<String?>(KEY_CATEGORY, null)

    val uiState: StateFlow<UiState<ActivityContent>> = combine(retryTick, month) { _, key -> key }
        .flatMapLatest { key ->
            combine(
                transactions.observeMonth(key),
                categories.observeCategories(),
                accounts.observeAccounts(),
                sources.observeSources(),
                selectedCategory,
            ) { month, allCategories, allAccounts, allSources, selected ->
                if (month.isEmpty()) UiState.Empty else UiState.Success(content(month, allCategories, allAccounts, allSources, selected))
            }
                .onStart<UiState<ActivityContent>> { emit(UiState.Loading) }
                .catch { emit(UiState.Error(it.toAppError())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun canGoNext(key: String): Boolean = key < time.currentMonthKey()

    fun onPreviousMonth() = moveTo(MonthKey.previous(month.value))

    fun onNextMonth() = moveTo(MonthKey.next(month.value)?.takeIf { canGoNext(month.value) })

    fun onCategorySelected(id: String?) {
        savedState[KEY_CATEGORY] = id
    }

    fun retry() = retryTick.update { it + 1 }

    private fun moveTo(key: String?) {
        if (key == null) return
        savedState[KEY_CATEGORY] = null // a category from another month may not be on the new list
        savedState[KEY_MONTH] = key
    }

    private fun content(
        month: List<Transaction>,
        allCategories: List<Category>,
        allAccounts: List<Account>,
        allSources: List<IncomeSource>,
        selected: String?,
    ): ActivityContent {
        val shown = if (selected == null) month else month.filter { it.categoryId == selected }
        val used = month.mapNotNull { it.categoryId }.toSet()
        return ActivityContent(
            days = shown
                .groupBy { it.date.atZone(time.zone).toLocalDate() }
                .map { (date, rows) -> ActivityDay(date, rows.spentMinor(), rows) },
            filters = allCategories.filter { it.id in used },
            selectedCategoryId = selected,
            categoriesById = allCategories.associateBy { it.id },
            accountsById = allAccounts.associateBy { it.id },
            sourcesById = allSources.associateBy { it.id },
            zone = time.zone,
        )
    }

    private companion object {
        const val KEY_MONTH = "activity_month"
        const val KEY_CATEGORY = "activity_category"
    }
}
