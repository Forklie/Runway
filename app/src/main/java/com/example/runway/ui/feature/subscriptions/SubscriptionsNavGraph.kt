package com.example.runway.ui.feature.subscriptions

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Subscription audit. AppNavHost calls this in one line. */
fun NavGraphBuilder.subscriptionsScreens(nav: NavController) {
    composable<SubscriptionsDest> {
        SubscriptionsRoute(onBack = { nav.popBackStack() })
    }
}
