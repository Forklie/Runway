package com.example.runway.ui.feature.insights

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.feature.checkin.openCheckin
import com.example.runway.ui.navigation.InsightsDest

/**
 * Insights tab. AppNavHost calls this in one line. The crypto button appears once the ledger route is
 * wired in (its action stays null until then).
 */
fun NavGraphBuilder.insightsScreens(nav: NavController) {
    composable<InsightsDest> {
        InsightsRoute(onCheckin = { nav.openCheckin() }, onCrypto = null)
    }
}
