package com.example.runway.ui.feature.crypto

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object CryptoDest

/** Crypto ledger, opened from Insights. AppNavHost calls this in one line. */
fun NavGraphBuilder.cryptoScreens(nav: NavController) {
    composable<CryptoDest> {
        CryptoRoute(onBack = { nav.popBackStack() })
    }
}

fun NavController.openCryptoLedger() {
    navigate(CryptoDest) { launchSingleTop = true }
}
