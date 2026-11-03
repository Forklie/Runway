package com.example.runway.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runway.core.network.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** App-wide facts the shell needs that are not about routing. Activity-scoped, like SessionViewModel. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    connectivity: ConnectivityObserver,
) : ViewModel() {

    /** Starts as true so the chip never flashes on launch. */
    val isOnline: StateFlow<Boolean> = connectivity.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}
