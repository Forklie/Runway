package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.runway.R
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Small 64dp bar for flows and forms: Back (or Close for modal editors, never both), title-2 and at
 * most one action. Flat on bg; draws its own status-bar inset (screens own their top inset).
 */
@Composable
fun SmallTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    close: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val c = RunwayTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.bg)
            .statusBarsPadding()
            .height(Dimens.topBar)
            .padding(horizontal = Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = if (close) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(if (close) R.string.ui_close else R.string.ui_back),
                tint = c.ink,
            )
        }
        Text(
            text = title,
            style = RunwayType.title2,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.invoke()
    }
}

/** Large title for main places (Insights): display style at the screen margin. Scrolls with content. */
@Composable
fun LargeTitle(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val c = RunwayTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Dimens.screenMargin, end = Spacing.s2, top = Spacing.s8, bottom = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(
            text = title,
            style = RunwayType.display,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        trailing?.invoke()
    }
}
