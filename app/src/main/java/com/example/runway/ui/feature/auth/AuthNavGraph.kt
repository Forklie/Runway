package com.example.runway.ui.feature.auth

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Auth screens. AppNavHost calls this in one line.
 * There is no "go home" here: signing in changes the session, and RunwayShell does the routing.
 */
fun NavGraphBuilder.authScreens(nav: NavController) {
    composable<LoginDest> {
        LoginRoute(
            onCreateAccount = { nav.navigate(RegisterDest) { launchSingleTop = true } },
            onForgotPassword = { nav.navigate(ResetPasswordDest) { launchSingleTop = true } },
        )
    }
    composable<RegisterDest> {
        RegisterRoute(
            onBack = { nav.popBackStack() },
            onHaveAccount = { nav.popBackStack() },
        )
    }
    composable<ResetPasswordDest> {
        ResetPasswordRoute(onBack = { nav.popBackStack() })
    }
}
