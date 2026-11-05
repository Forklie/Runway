package com.example.runway.ui.feature.goal

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.navigation.GoalDest
import kotlinx.serialization.Serializable

// Feature-internal route. No arguments: the form loads the one active goal itself (BR-70).
@Serializable data object GoalEditDest

/** Goal tab and its form. AppNavHost calls this in one line. The form hides the bottom bar. */
fun NavGraphBuilder.goalScreens(nav: NavController) {
    composable<GoalDest> {
        GoalRoute(onEditGoal = { nav.navigate(GoalEditDest) { launchSingleTop = true } })
    }
    composable<GoalEditDest> {
        GoalEditRoute(onDone = { nav.popBackStack() })
    }
}
