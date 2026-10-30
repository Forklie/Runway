package com.example.runway.ui.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.feature.checkin.openCheckin
import com.example.runway.ui.feature.settings.SettingsDest
import com.example.runway.ui.feature.income.openLogIncome
import com.example.runway.ui.navigation.ActivityDest
import com.example.runway.ui.navigation.GoalDest
import com.example.runway.ui.navigation.HomeDest
import com.example.runway.ui.navigation.navigateTopLevel

/**
 * Home. AppNavHost calls this in one line. Actions whose screens do not exist yet stay null, so their
 * buttons are hidden instead of doing nothing: Quick Add, Income and Transfer arrive with A's slice.
 */
fun NavGraphBuilder.homeScreens(nav: NavController) {
    composable<HomeDest> {
        HomeRoute(
            actions = HomeActions(
                onOpenGoal = { nav.navigateTopLevel(GoalDest) },
                onSeeActivity = { nav.navigateTopLevel(ActivityDest) },
                onCheckin = { nav.openCheckin() },
                onIncome = { nav.openLogIncome() },
                onSettings = { nav.navigate(SettingsDest) { launchSingleTop = true } },
            ),
        )
    }
}
