package com.example.runway.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A label over a button showing the date. Tapping it opens the picker. */
@Composable
fun DateField(label: String, text: String, selected: LocalDate, onPick: (LocalDate) -> Unit, latest: LocalDate? = null) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(label, style = RunwayType.label, color = RunwayTheme.colors.ink)
        RunwayButton(text = text, onClick = { picking = true }, style = ButtonStyle.Secondary, size = ButtonSize.Small)
    }
    if (picking) {
        RunwayDatePicker(
            selected = selected,
            latest = latest,
            onPick = {
                onPick(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** Material's picker in LocalDate terms (it works in UTC millis). [latest] caps the choice: back-dating only (BR-18). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunwayDatePicker(
    selected: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    latest: LocalDate? = null,
) {
    val latestMillis = latest?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            RunwayButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    state.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        ?.let(onPick) ?: onDismiss()
                },
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Small,
            )
        },
        dismissButton = {
            RunwayButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Small,
            )
        },
    ) {
        DatePicker(state = state)
    }
}
