package com.example.runway.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.runway.ui.theme.RunwayTheme

/**
 * Root composable. Owns the bottom bar and the bottom inset only.
 * Screens own their TOP inset: the hero panel or top app bar draws under the status bar
 * (use statusBarsPadding()). Bottom CTAs and sheets add imePadding().
 * The bar hides on flow screens (onboarding, check-in, forms).
 */
@Composable
fun RunwayShell() {
    val colors = RunwayTheme.colors
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { currentDestination.isRouteInHierarchy(it.routeClass) }

    Scaffold(
        containerColor = colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { navController.navigateTopLevel(it.route) },
                    onQuickAdd = { /* A: navController.navigate(QuickAddDest) */ },
                )
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .then(
                    if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                    else Modifier.navigationBarsPadding(),
                ),
        )
    }
}

fun NavController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}