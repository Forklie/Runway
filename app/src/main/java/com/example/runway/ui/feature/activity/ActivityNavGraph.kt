package com.example.runway.ui.feature.activity

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.runway.ui.navigation.ActivityDest
import kotlinx.serialization.Serializable

// Feature-internal routes. Detail and Transfer are flow screens, so the bottom bar hides.
@Serializable data class TransactionDetailDest(val id: String)
@Serializable data object TransferDest

/** Activity tab, one transaction's detail, and Transfer. AppNavHost calls this in one line. */
fun NavGraphBuilder.activityScreens(nav: NavController) {
    composable<ActivityDest> {
        ActivityRoute(onOpen = { nav.navigate(TransactionDetailDest(it)) { launchSingleTop = true } })
    }
    composable<TransactionDetailDest> {
        TransactionDetailRoute(onDone = { nav.popBackStack() })
    }
    composable<TransferDest> {
        TransferRoute(onDone = { nav.popBackStack() })
    }
}
