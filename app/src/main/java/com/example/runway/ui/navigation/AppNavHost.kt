package com.example.runway.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.runway.R

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
    startDestination: Any = HomeDest, // Day 2: SessionViewModel picks Login / Onboarding / Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // B: auth, onboarding, settings, subscriptions (Day 2+)

        // A: replace with activityScreens(navController). Income, Transfer and Quick Add also live here.
        composable<ActivityDest> { PlaceholderScreen(R.string.nav_activity) }

        // C: replace with homeScreens, goalScreens and insightsScreens
        composable<HomeDest> { PlaceholderScreen(R.string.nav_home) }
        composable<GoalDest> { PlaceholderScreen(R.string.nav_goal) }
        composable<InsightsDest> { PlaceholderScreen(R.string.nav_insights) }
    }
}