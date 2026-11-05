package com.example.runway.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.Spacing

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFF4F4FD, widthDp = 360)
@Composable
private fun ComponentsLight() = ComponentsSheet(dark = false)

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF0B0A26, widthDp = 360)
@Composable
private fun ComponentsDark() = ComponentsSheet(dark = true)

@Composable
private fun ComponentsSheet(dark: Boolean) {
    RunwayTheme(darkTheme = dark) {
        Column(
            modifier = Modifier.padding(Spacing.s5),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            RunwayButton("Save expense", onClick = {}, size = ButtonSize.Large, fillWidth = true)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                RunwayButton("Edit", onClick = {}, style = ButtonStyle.Secondary)
                RunwayButton("Income", onClick = {}, style = ButtonStyle.Tonal)
                RunwayButton("Skip for now", onClick = {}, style = ButtonStyle.Tertiary)
            }
            AmountField(value = "1250.5", onValueChange = {}, label = "Amount")
            AmountField(value = "", onValueChange = {}, label = "Amount", errorText = "Enter an amount above 0.")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                CategoryChip("Food", selected = true, onClick = {})
                CategoryChip("Transport", selected = false, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                MoneyText(120_000L, kind = MoneyKind.Expense)
                MoneyText(3_624_000L, kind = MoneyKind.Income)
            }
            EmptyState(
                lockup = listOf(
                    LockupWord("nothing", WordShape.Box),
                    LockupWord("here", WordShape.Pill),
                    LockupWord("yet", WordShape.Arrow),
                ),
                title = "Nothing logged today",
                body = "Add it whenever you remember. Sunday's balance check keeps your totals right either way.",
            )
        }
    }
}