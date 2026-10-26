package com.example.runway.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.runway.R
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

// Bottom-bar tabs (design system: Home, Activity, [Quick Add], Goal, Insights).
// Declared here because the bar needs them. Feature-internal routes stay in each feature folder.
@Serializable data object HomeDest
@Serializable data object ActivityDest
@Serializable data object GoalDest
@Serializable data object InsightsDest

data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    val icon: ImageVector,         // outlined when inactive
    val selectedIcon: ImageVector, // filled when active
)

val topLevelDestinations = listOf(
    TopLevelDestination(HomeDest, HomeDest::class, R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
    TopLevelDestination(ActivityDest, ActivityDest::class, R.string.nav_activity, Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Rounded.ReceiptLong),
    TopLevelDestination(GoalDest, GoalDest::class, R.string.nav_goal, Icons.Outlined.Flag, Icons.Rounded.Flag),
    TopLevelDestination(InsightsDest, InsightsDest::class, R.string.nav_insights, Icons.Outlined.Insights, Icons.Rounded.Insights),
)