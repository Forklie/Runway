package com.example.runway.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.runway.ui.feature.activity.activityScreens
import com.example.runway.ui.feature.auth.authScreens
import com.example.runway.ui.feature.checkin.checkinScreens
import com.example.runway.ui.feature.crypto.cryptoScreens
import com.example.runway.ui.feature.goal.goalScreens
import com.example.runway.ui.feature.home.homeScreens
import com.example.runway.ui.feature.income.incomeScreens
import com.example.runway.ui.feature.insights.insightsScreens
import com.example.runway.ui.feature.onboarding.onboardingScreens
import com.example.runway.ui.feature.settings.settingsScreens
import com.example.runway.ui.feature.subscriptions.subscriptionsScreens

/**
 * MERGE HOTSPOT (owner: B). One line per feature graph, nothing else.
 * Each feature declares `fun NavGraphBuilder.xxxScreens(nav: NavController)` in its own folder
 * and this file only calls it. To replace a placeholder, delete its composable<...> line and
 * add the call (e.g. homeScreens(navController)).
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = HomeDest, // RunwayShell passes the route SessionViewModel picked
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // B: auth (Day 2), onboarding, settings and subscriptions (Day 3).
        authScreens(navController)
        onboardingScreens(navController)
        settingsScreens(navController)
        subscriptionsScreens(navController)

        // A: Freelance also lives here.
        activityScreens(navController)
        incomeScreens(navController)

        // C: replace with homeScreens, goalScreens and insightsScreens
        homeScreens(navController)
        goalScreens(navController)
        insightsScreens(navController)
        checkinScreens(navController)
        cryptoScreens(navController)
    }
}
