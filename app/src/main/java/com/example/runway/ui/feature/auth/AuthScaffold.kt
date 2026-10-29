package com.example.runway.ui.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.runway.R
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.WordLockup
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Shared frame of Login, Register and Reset (design system "Form" layout): small top bar with Back
 * (not on Login), word lockup, title-1 and a calm line, fields 24dp apart on 20dp margins, and one
 * Large primary pinned at the bottom that rises with the keyboard. The 600dp column keeps it tidy
 * on tablets and in landscape.
 */
@Composable
fun AuthScaffold(
    lockup: List<LockupWord>,
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    submitting: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RunwayTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(Dimens.topBar).padding(horizontal = Spacing.s2)) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.auth_back),
                        tint = colors.ink,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = Dimens.contentMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenMargin),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            WordLockup(lockup)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                Text(
                    text = title,
                    style = RunwayType.title1,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text = subtitle, style = RunwayType.body, color = colors.inkMuted)
            }
            content()
        }

        Column(
            modifier = Modifier
                .widthIn(max = Dimens.contentMax)
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenMargin)
                .padding(top = Spacing.s3, bottom = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RunwayButton(
                text = primaryLabel,
                onClick = onPrimary,
                size = ButtonSize.Large,
                fillWidth = true,
                loading = submitting,
            )
            footer()
        }
    }
}

/** A problem that is not about one field (wrong password, offline). Announced by TalkBack. */
@Composable
fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    val colors = RunwayTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RunwayShape.Field)
            .border(Dimens.stroke, colors.danger, RunwayShape.Field)
            .padding(Spacing.s4)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSm),
            tint = colors.danger,
        )
        Text(text = message, style = RunwayType.bodySm, color = colors.ink)
    }
}

/** Neutral confirmation after a reset request: the same words whether or not the email has an account. */
@Composable
fun AuthNotice(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = RunwayTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.accentSoft, RunwayShape.Card)
            .padding(Dimens.cardPadding)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconMd),
            tint = colors.accent,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(text = title, style = RunwayType.label, color = colors.ink)
            Text(text = body, style = RunwayType.bodySm, color = colors.inkMuted)
        }
    }
}
