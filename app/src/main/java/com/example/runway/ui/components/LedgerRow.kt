package com.example.runway.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.runway.R
import com.example.runway.core.money.spokenAmount
import com.example.runway.domain.model.Account
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.ZoneId

/** Maps one transaction to the design system's list row: who, what, where, when. Home and Activity share it. */
@Composable
fun LedgerRow(
    tx: Transaction,
    categoriesById: Map<String, Category>,
    accountsById: Map<String, Account>,
    sourcesById: Map<String, IncomeSource>,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    val category = tx.categoryId?.let(categoriesById::get)
    val account = tx.accountId?.let(accountsById::get)
    val time = tx.date.timeLabel(zone)
    val uncategorised = stringResource(R.string.home_uncategorised)
    val transferLabel = stringResource(R.string.tx_caption_transfer)
    val refundLabel = stringResource(R.string.tx_caption_refund)

    val title: String
    val details: List<String>
    val icon: ImageVector
    val kind: MoneyKind
    var caption: String? = null
    when (tx.type) {
        TransactionType.EXPENSE -> {
            title = tx.note.ifBlank { category?.name ?: uncategorised }
            details = listOfNotNull(category?.name, account?.name, time)
            icon = categoryIcon(category?.iconKey)
            kind = MoneyKind.Expense
        }
        TransactionType.INCOME -> {
            val source = tx.incomeSourceId?.let(sourcesById::get)
            title = tx.note.ifBlank { source?.name ?: stringResource(R.string.home_row_income) }
            details = listOfNotNull(source?.name, account?.name, time)
            icon = Icons.Rounded.Payments
            kind = MoneyKind.Income
        }
        TransactionType.TRANSFER -> {
            val to = tx.toAccountId?.let(accountsById::get)
            title = tx.note.ifBlank { transferLabel }
            details = listOf(stringResource(R.string.home_row_transfer_between, account?.name.orEmpty(), to?.name.orEmpty()), time)
            icon = Icons.Rounded.SwapHoriz
            kind = MoneyKind.Plain
            caption = transferLabel
        }
        TransactionType.REFUND -> {
            title = tx.note.ifBlank { category?.name ?: refundLabel }
            details = listOfNotNull(category?.name, account?.name, time)
            icon = Icons.AutoMirrored.Rounded.Undo
            kind = MoneyKind.Income
            caption = refundLabel
        }
    }
    val subtitle = details.joinToString(" · ")
    TransactionRow(
        title = title,
        subtitle = subtitle,
        amountMinor = tx.amountLkrMinor,
        kind = kind,
        icon = icon,
        spoken = listOf(title, spokenAmount(tx.amountLkrMinor, expense = tx.type == TransactionType.EXPENSE), subtitle)
            .joinToString(", "),
        modifier = modifier,
        caption = caption,
    )
}
