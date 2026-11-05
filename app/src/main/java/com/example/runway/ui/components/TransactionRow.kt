package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * One transaction (design system, List row): 44dp tile, title and subtitle, the amount right-aligned
 * in tabular figures, at least 72dp tall. Expense "− 1,200" in ink, income "+ 36,240" in accent,
 * transfers plain with "Transfer" under them. One TalkBack description for the whole row.
 */
@Composable
fun TransactionRow(
    title: String,
    subtitle: String,
    amountMinor: Long,
    kind: MoneyKind,
    icon: ImageVector,
    spoken: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val c = RunwayTheme.colors
    val incomeLike = kind == MoneyKind.Income
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.listRow)
            .padding(start = Spacing.s4, end = Dimens.screenMargin, top = Spacing.s3, bottom = Spacing.s3)
            .clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.tile)
                .background(if (incomeLike) c.accentSoft else c.surfaceSunken, RunwayShape.Keycap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSm), tint = if (incomeLike) c.accent else c.ink)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = RunwayType.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = RunwayType.bodySm, color = c.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(amountMinor = amountMinor, kind = kind, cents = amountMinor % 100 != 0L) // BR-08
            if (caption != null) Text(caption, style = RunwayType.caption, color = c.inkMuted)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TransactionRowPreview() {
    RunwayTheme {
        TransactionRow(
            title = "Kottu at Pilawoos",
            subtitle = "Food · Cash · 8:42 pm",
            amountMinor = 120_000,
            kind = MoneyKind.Expense,
            icon = Icons.Rounded.Restaurant,
            spoken = "Kottu at Pilawoos, minus 1,200 rupees, food, cash, 8:42 PM",
        )
    }
}
