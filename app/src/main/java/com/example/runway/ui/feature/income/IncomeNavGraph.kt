package com.example.runway.ui.feature.income

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

// A form with no arguments: it loads the sources and accounts itself. The bottom bar hides.
@Serializable data object LogIncomeDest

/** Log income. AppNavHost calls this in one line. */
fun NavGraphBuilder.incomeScreens(nav: NavController) {
    composable<LogIncomeDest> {
        LogIncomeRoute(onDone = { nav.popBackStack() })
    }
}

/** Opens Log income from Home's action row. */
fun NavController.openLogIncome() {
    navigate(LogIncomeDest) { launchSingleTop = true }
}
