package com.example.runway.ui.feature.checkin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

// No arguments: the flow loads the accounts and their expected balances itself.
@Serializable data object CheckinDest

/** Weekly check-in flow. AppNavHost calls this in one line. A flow screen, so the bottom bar hides. */
fun NavGraphBuilder.checkinScreens(nav: NavController) {
    composable<CheckinDest> {
        CheckinRoute(onDone = { nav.popBackStack() })
    }
}

/** Opens the check-in from Home's action row, the welcome-back card or Insights. */
fun NavController.openCheckin() {
    navigate(CheckinDest) { launchSingleTop = true }
}
