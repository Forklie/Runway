package com.example.runway.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/** A label over a wrapping row of chips, one selected. [choices] are id to name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(label: String, choices: List<Pair<String, String>>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(label, style = RunwayType.label, color = RunwayTheme.colors.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            choices.forEach { (id, name) ->
                CategoryChip(label = name, selected = id == selected, onClick = { onSelect(id) })
            }
        }
    }
}
