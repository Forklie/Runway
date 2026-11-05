package com.example.runway.ui.feature.insights

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.feature.checkin.openCheckin
import com.example.runway.ui.feature.crypto.openCryptoLedger
import com.example.runway.ui.navigation.InsightsDest

/** Insights tab. AppNavHost calls this in one line. */
fun NavGraphBuilder.insightsScreens(nav: NavController) {
    composable<InsightsDest> {
        InsightsRoute(onCheckin = { nav.openCheckin() }, onCrypto = { nav.openCryptoLedger() })
    }
}
