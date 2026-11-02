package com.example.runway.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.runway.core.money.formatAmount
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType

/** Eyebrow header above a group ("TODAY", "THIS MONTH"), with the group total on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, totalMinor: Long? = null) {
    val c = RunwayTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title.uppercase(),
            style = RunwayType.eyebrow,
            color = c.inkMuted,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (totalMinor != null) {
            Text(totalMinor.formatAmount(), style = RunwayType.money.copy(fontSize = RunwayType.label.fontSize), color = c.inkMuted)
        }
    }
}
