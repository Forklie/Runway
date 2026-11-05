package com.example.runway.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.spokenAmount
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

data class BarItem(val label: String, val valueMinor: Long, val icon: ImageVector? = null)

/**
 * Ranked horizontal bars ("Food and Coffee: LKR 24,000"). Bars are drawn behind a fixed-height box,
 * scaled to the largest item, so the biggest category always fills the width. Each row reads as
 * "label, amount" for TalkBack.
 */
@Composable
fun BarList(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    barColor: Color = RunwayTheme.colors.accent,
) {
    val c = RunwayTheme.colors
    val max = items.maxOfOrNull { it.valueMinor }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        items.forEach { item ->
            val spoken = item.label + ", " + spokenAmount(item.valueMinor, expense = false)
            Row(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.icon != null) {
                    Box(
                        modifier = Modifier.size(Dimens.tile).background(c.surfaceSunken, RunwayShape.Keycap),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(Dimens.iconSm), tint = c.ink)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.label,
                            style = RunwayType.label,
                            color = c.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(item.valueMinor.formatAmount(), style = RunwayType.money.copy(fontSize = RunwayType.label.fontSize), color = c.ink)
                    }
                    val share = item.valueMinor.toFloat() / max
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .drawBehind {
                                val radius = CornerRadius(size.height / 2)
                                drawRoundRect(c.surfaceSunken, cornerRadius = radius)
                                drawRoundRect(barColor, size = Size(size.width * share, size.height), cornerRadius = radius)
                            },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BarListPreview() {
    RunwayTheme {
        BarList(
            items = listOf(
                BarItem("Rent", 3_400_000, Icons.Rounded.Home),
                BarItem("Food and Coffee", 2_400_000, Icons.Rounded.Restaurant),
                BarItem("Transport", 350_000),
            ),
        )
    }
}
