package com.example.runway.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.runway.R
import com.example.runway.ui.feature.auth.authScreens
import com.example.runway.ui.feature.home.homeScreens
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

        // A: replace with activityScreens(navController). Income, Transfer and Quick Add also live here.
        composable<ActivityDest> { PlaceholderScreen(R.string.nav_activity) }

        // C: replace with homeScreens, goalScreens and insightsScreens
        homeScreens(navController)
        composable<GoalDest> { PlaceholderScreen(R.string.nav_goal) }
        composable<InsightsDest> { PlaceholderScreen(R.string.nav_insights) }
    }
}
