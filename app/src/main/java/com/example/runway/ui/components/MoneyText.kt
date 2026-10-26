package com.example.runway.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.example.runway.core.money.expenseText
import com.example.runway.core.money.formatAmount
import com.example.runway.core.money.formatLkr
import com.example.runway.core.money.incomeText
import com.example.runway.core.money.spokenAmount
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType

enum class MoneyKind { Plain, Expense, Income }

/**
 * All money on screen goes through here. Input is Long minor units. Uses a tabular-figure style
 * (RunwayType.money / moneyLg / moneyHero). Expense: "− 1,200" in ink. Income: "+ 36,240" in accent.
 * Transfers use Plain (no sign). Has a spoken form for TalkBack.
 */
@Composable
fun MoneyText(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    kind: MoneyKind = MoneyKind.Plain,
    style: TextStyle = RunwayType.money,
    color: Color = Color.Unspecified,
    cents: Boolean = false,
    showCurrency: Boolean = false,
) {
    val c = RunwayTheme.colors
    val text = when (kind) {
        MoneyKind.Expense -> expenseText(amountMinor, cents)
        MoneyKind.Income -> incomeText(amountMinor, cents)
        MoneyKind.Plain ->
            if (showCurrency) amountMinor.formatLkr(cents) else amountMinor.formatAmount(cents)
    }
    val spoken = spokenAmount(amountMinor, expense = kind == MoneyKind.Expense, cents = cents)
    val resolved = when {
        color != Color.Unspecified -> color
        kind == MoneyKind.Income -> c.accent
        else -> c.ink
    }
    Text(
        text = text,
        modifier = modifier.semantics { contentDescription = spoken },
        style = style,
        color = resolved,
        maxLines = 1,
        softWrap = false,
    )
}