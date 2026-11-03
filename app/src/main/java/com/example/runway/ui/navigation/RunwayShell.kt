package com.example.runway.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.runway.R
import com.example.runway.core.event.AppEvent
import com.example.runway.core.event.AppEvents
import com.example.runway.ui.components.OfflineChip
import com.example.runway.ui.feature.auth.authDestinations
import com.example.runway.ui.feature.onboarding.OnboardingDest
import com.example.runway.ui.feature.session.RouteGroup
import com.example.runway.ui.feature.session.SessionProblemScreen
import com.example.runway.ui.feature.session.SessionState
import com.example.runway.ui.feature.session.SessionViewModel
import com.example.runway.ui.feature.session.routeGroupOf
import com.example.runway.ui.feature.session.startDestination
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.Spacing

/**
 * Root composable. Owns the bottom bar, the bottom inset and session routing.
 * Screens own their TOP inset: the hero panel or top app bar draws under the status bar
 * (use statusBarsPadding()). Bottom CTAs and sheets add imePadding().
 * The bar (and the Quick Add button on it) hides on auth routes and on flow screens
 * (onboarding, check-in, forms).
 */
@Composable
fun RunwayShell(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val colors = RunwayTheme.colors
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Offline chip (FR-04) and the app-wide snackbar for writes the server refused (NFR-03).
    val isOnline by shellViewModel.isOnline.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val rejectedMessage = stringResource(R.string.write_rejected_message)
    LaunchedEffect(Unit) {
        AppEvents.events.collect { event ->
            when (event) {
                AppEvent.WriteRejected -> snackbarHostState.showSnackbar(rejectedMessage)
            }
        }
    }

    // The first route we can start on (Login or Home). The splash screen covers the wait.
    var firstRoute by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(session) {
        if (firstRoute == null) firstRoute = session.startDestination()
    }

    // After the first route, a changed session moves the user between the three groups (auth,
    // onboarding, main): sign-in leaves Login, finishing onboarding leaves it for Home, sign-out leaves the app.
    LaunchedEffect(session, firstRoute) {
        val target = session.startDestination() ?: return@LaunchedEffect
        if (firstRoute == null) return@LaunchedEffect
        val current = navController.currentDestination ?: return@LaunchedEffect
        if (routeGroupOf(target) != current.routeGroup()) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = !currentDestination.isAuthRoute() &&
        topLevelDestinations.any { currentDestination.isRouteInHierarchy(it.routeClass) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
            firstRoute?.let { start ->
                AppNavHost(
                    navController = navController,
                    startDestination = start,
                    modifier = Modifier
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .then(
                            if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                            else Modifier.navigationBarsPadding(),
                        ),
                )
            }
        }

        // Saved on this phone: shown on every screen except sign-in while there is no network.
        if (!isOnline && !currentDestination.isAuthRoute()) {
            OfflineChip(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = Spacing.s2),
            )
        }

        // Signed in, but the profile could not be loaded or repaired (usually offline).
        (session as? SessionState.Problem)?.let { problem ->
            SessionProblemScreen(error = problem.error, onRetry = sessionViewModel::retry)
        }
    }
}

private fun NavDestination?.isAuthRoute(): Boolean =
    authDestinations.any { isRouteInHierarchy(it) }

private fun NavDestination?.routeGroup(): RouteGroup = when {
    isAuthRoute() -> RouteGroup.Auth
    isRouteInHierarchy(OnboardingDest::class) -> RouteGroup.Onboarding
    else -> RouteGroup.Main
}

fun NavController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
