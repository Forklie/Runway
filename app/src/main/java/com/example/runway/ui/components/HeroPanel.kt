package com.example.runway.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.core.money.formatAmount
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

private val PillRing = 4.dp // bg ring so the pill reads as cut out of the panel

/**
 * The azul band at the top of Home and Goal (design system "Hero panel"): edge to edge, drawn under
 * the status bar, 32dp bottom corners, the screen's one loud thing. [pill] sits centred on the
 * bottom edge, half over it. The panel never holds a list or a form: number, context, pill, done.
 */
@Composable
fun HeroPanel(
    topRow: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    pill: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = RunwayTheme.colors
    Box(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(bottom = if (pill != null) (Dimens.overlapPill + PillRing * 2) / 2 else 0.dp)
                .fillMaxWidth()
                .background(c.brandBlock, RunwayShape.HeroPanel)
                .statusBarsPadding()
                .padding(horizontal = Dimens.screenMargin)
                .padding(bottom = Spacing.s10),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(Dimens.topBar),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                content = topRow,
            )
            content()
        }
        if (pill != null) {
            Box(Modifier.align(Alignment.BottomCenter)) { pill() }
        }
    }
}

/** Edge pill: caption text and an 18dp icon, max 24 characters. Home uses highlight, Goal uses goal. */
@Composable
fun EdgePill(
    text: String,
    icon: ImageVector,
    fill: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(RunwayTheme.colors.bg, CircleShape)
            .padding(PillRing)
            .height(Dimens.overlapPill)
            .background(fill, CircleShape)
            .padding(horizontal = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = content)
        Text(text, style = RunwayType.caption, color = content, maxLines = 1)
    }
}

/** Round 48dp icon button filled on-brand-soft (Settings, Edit). */
@Composable
fun HeroIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    Box(
        modifier = modifier
            .size(Dimens.touchTarget)
            .clip(CircleShape)
            .background(c.onBrandSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconMd), tint = c.onBrand)
    }
}

/** The 40dp outlined keycap with the user's initial (Home top row). Decorative: the greeting says the name. */
@Composable
fun InitialKeycap(initial: String, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    Box(
        modifier = modifier.size(40.dp).border(Dimens.stroke, c.onBrand, RunwayShape.Keycap),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, style = RunwayType.labelLg, color = c.onBrand)
    }
}

/**
 * "LKR 1,840": the number in money-hero, the currency small and muted. Counts up once per screen
 * visit (360ms), then shows the exact value; rememberSaveable keeps it still after rotation.
 */
@Composable
fun HeroAmount(amountMinor: Long, spoken: String, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    var counted by rememberSaveable { mutableStateOf(false) }
    val shown = remember { Animatable(if (counted) amountMinor.toFloat() else 0f) }
    LaunchedEffect(amountMinor) {
        if (!counted) shown.animateTo(amountMinor.toFloat(), tween(360))
        counted = true
    }
    val value = if (shown.isRunning) shown.value.toLong() else amountMinor

    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(
            stringResource(R.string.ui_currency_lkr),
            style = RunwayType.label,
            color = c.onBrandMuted,
            modifier = Modifier.alignByBaseline(),
        )
        Text(value.formatAmount(), style = RunwayType.moneyHero, color = c.onBrand, modifier = Modifier.alignByBaseline())
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HeroPanelPreview() {
    RunwayTheme {
        val c = RunwayTheme.colors
        HeroPanel(
            topRow = {
                InitialKeycap("K")
                Column(Modifier.weight(1f)) {
                    Text("hi Kavindu", style = RunwayType.title2, color = c.onBrand)
                    Text("Sunday 25 Oct", style = RunwayType.caption, color = c.onBrandMuted)
                }
                HeroIconButton(Icons.Rounded.Settings, "Settings", onClick = {})
            },
            pill = { EdgePill("On pace this month", Icons.AutoMirrored.Rounded.TrendingUp, c.highlight, c.onHighlight) },
        ) {
            Text("SAFE TO SPEND TODAY", style = RunwayType.eyebrow, color = c.onBrandMuted)
            HeroAmount(184_000, spoken = "1,840 rupees a day")
            Text("a day until 30 Nov", style = RunwayType.bodySm, color = c.onBrandMuted)
        }
    }
}
