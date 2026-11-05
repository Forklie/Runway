package com.example.runway.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.runway.ui.components.QuickAddButton
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import kotlin.reflect.KClass

private val QuickAddRaise = 36.dp // button rises 36dp above the bar's top edge
private val QuickAddRing = 6.dp   // surface ring so it reads as cut out of the bar

/**
 * 80dp bar plus the gesture inset, surface fill, 1dp line on top.
 * Five equal slots: Home, Activity, [Quick Add], Goal, Insights.
 * The button sits in a transparent overhang above the bar, so taps on it are delivered.
 */
@Composable
fun BottomBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunwayTheme.colors
    val overhang = QuickAddRaise + QuickAddRing
    val left = topLevelDestinations.take(2)
    val right = topLevelDestinations.drop(2)

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = colors.surface,
            modifier = Modifier
                .padding(top = overhang)
                .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(Dimens.hairline).background(colors.line))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.bottomNav - Dimens.hairline)
                        .padding(top = Spacing.s3),
                ) {
                    left.forEach { dest ->
                        BottomBarItem(
                            destination = dest,
                            selected = currentDestination.isRouteInHierarchy(dest.routeClass),
                            onClick = { onNavigate(dest) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.weight(1f)) // centre slot belongs to Quick Add
                    right.forEach { dest ->
                        BottomBarItem(
                            destination = dest,
                            selected = currentDestination.isRouteInHierarchy(dest.routeClass),
                            onClick = { onNavigate(dest) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(Dimens.quickAdd + QuickAddRing * 2)
                .background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            QuickAddButton(onClick = onQuickAdd)
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RunwayTheme.colors
    val label = stringResource(destination.labelRes)
    val indicator by animateColorAsState(
        targetValue = if (selected) colors.accentSoft else Color.Transparent,
        animationSpec = tween(200),
        label = "navIndicator",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(indicator, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconMd),
                tint = if (selected) colors.accent else colors.inkMuted,
            )
        }
        Text(
            text = label,
            style = RunwayType.caption,
            color = if (selected) colors.ink else colors.inkMuted,
            maxLines = 1,
        )
    }
}

fun NavDestination?.isRouteInHierarchy(route: KClass<*>): Boolean =
    this?.hierarchy?.any { it.hasRoute(route) } == true