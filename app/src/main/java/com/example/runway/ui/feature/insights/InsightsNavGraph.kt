package com.example.runway.ui.feature.insights

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.navigation.InsightsDest

/**
 * Insights tab. AppNavHost calls this in one line. The check-in and crypto buttons appear when those
 * screens are wired in (their actions stay null until then).
 */
@Suppress("UNUSED_PARAMETER") // nav is used once the check-in and crypto routes land
fun NavGraphBuilder.insightsScreens(nav: NavController) {
    composable<InsightsDest> {
        InsightsRoute(onCheckin = null, onCrypto = null)
    }
}
