package com.example.runway.ui.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Settings screens. AppNavHost calls this in one line.
 * Sign out has no navigation here: the auth state changes and RunwayShell sends the user to Login.
 */
fun NavGraphBuilder.settingsScreens(nav: NavController) {
    composable<SettingsDest> {
        SettingsRoute(onBack = { nav.popBackStack() })
    }
}
