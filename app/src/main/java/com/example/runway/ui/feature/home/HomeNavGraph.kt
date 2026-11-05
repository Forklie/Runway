package com.example.runway.ui.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.feature.activity.TransferDest
import com.example.runway.ui.feature.checkin.openCheckin
import com.example.runway.ui.feature.freelance.FreelanceDest
import com.example.runway.ui.feature.freelance.OverdueMilestonesCard
import com.example.runway.ui.feature.freelance.openProject
import com.example.runway.ui.feature.quickadd.QuickAddViewModel
import com.example.runway.ui.feature.settings.SettingsDest
import com.example.runway.ui.feature.income.LogIncomeDest
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
        // The shell's Quick Add: same activity-scoped ViewModel, so the same sheet and Undo (AT-01).
        val quickAdd: QuickAddViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
        HomeRoute(
            actions = HomeActions(
                onAddExpense = quickAdd::onOpen,
                onOpenGoal = { nav.navigateTopLevel(GoalDest) },
                onSeeActivity = { nav.navigateTopLevel(ActivityDest) },
                onCheckin = { nav.openCheckin() },
                onFreelance = { nav.navigate(FreelanceDest) { launchSingleTop = true } },
                onIncome = { nav.navigate(LogIncomeDest) { launchSingleTop = true } },
                onTransfer = { nav.navigate(TransferDest) { launchSingleTop = true } },
                onSettings = { nav.navigate(SettingsDest) { launchSingleTop = true } },
                overdueCard = { OverdueMilestonesCard(onOpenProject = { nav.openProject(it) }) },
            ),
        )
    }
}
