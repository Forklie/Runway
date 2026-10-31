package com.example.runway.ui.feature.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.feature.activity.TransferDest
import com.example.runway.ui.feature.checkin.openCheckin
import com.example.runway.ui.feature.settings.SettingsDest
import com.example.runway.ui.navigation.ActivityDest
import com.example.runway.ui.navigation.GoalDest
import com.example.runway.ui.navigation.HomeDest
import com.example.runway.ui.navigation.navigateTopLevel

/**
 * Home. AppNavHost calls this in one line. Actions whose screens do not exist yet stay null, so their
 * buttons are hidden instead of doing nothing.
 */
fun NavGraphBuilder.homeScreens(nav: NavController) {
    composable<HomeDest> {
        HomeRoute(
            actions = HomeActions(
                onOpenGoal = { nav.navigateTopLevel(GoalDest) },
                onSeeActivity = { nav.navigateTopLevel(ActivityDest) },
                onCheckin = { nav.openCheckin() },
                onTransfer = { nav.navigate(TransferDest) { launchSingleTop = true } },
                onSettings = { nav.navigate(SettingsDest) { launchSingleTop = true } },
            ),
        )
    }
}
