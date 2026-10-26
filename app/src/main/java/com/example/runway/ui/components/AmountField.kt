package com.example.runway.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.runway.core.money.MoneyParser
import com.example.runway.ui.theme.RunwayType

/**
 * Money input. Holds a String (never a Double), filters keystrokes with MoneyParser.sanitizeInput,
 * uses tabular figures. The ViewModel validates with MoneyParser.parseToMinor on Save and shows
 * R.string.amount_error_invalid when it returns null.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    currencyCode: String = "LKR",
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    RunwayTextField(
        value = value,
        onValueChange = { onValueChange(MoneyParser.sanitizeInput(it)) },
        label = label,
        modifier = modifier,
        prefix = currencyCode,
        placeholder = "0",
        helperText = helperText,
        errorText = errorText,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = RunwayType.body.copy(fontFeatureSettings = "tnum"),
    )
}