package com.example.runway.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong as ReceiptLongOutlined
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong as ReceiptLongRounded
import androidx.compose.material.icons.outlined.Flag as FlagOutlined
import androidx.compose.material.icons.outlined.Home as HomeOutlined
import androidx.compose.material.icons.outlined.Insights as InsightsOutlined
import androidx.compose.material.icons.rounded.Flag as FlagRounded
import androidx.compose.material.icons.rounded.Home as HomeRounded
import androidx.compose.material.icons.rounded.Insights as InsightsRounded
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
    TopLevelDestination(HomeDest, HomeDest::class, R.string.nav_home, HomeOutlined, HomeRounded),
    TopLevelDestination(ActivityDest, ActivityDest::class, R.string.nav_activity, ReceiptLongOutlined, ReceiptLongRounded),
    TopLevelDestination(GoalDest, GoalDest::class, R.string.nav_goal, FlagOutlined, FlagRounded),
    TopLevelDestination(InsightsDest, InsightsDest::class, R.string.nav_insights, InsightsOutlined, InsightsRounded),
)