package com.example.runway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * "Saved on this phone". Calm, not an error: offline work is safe and syncs later (zero-guilt copy).
 * Announced politely to TalkBack when it appears. RunwayShell shows it while there is no network.
 */
@Composable
fun OfflineChip(modifier: Modifier = Modifier) {
    val colors = RunwayTheme.colors
    Row(
        modifier = modifier
            .background(colors.surface, CircleShape)
            .border(Dimens.stroke, colors.lineStrong, CircleShape)
            .padding(horizontal = Spacing.s3, vertical = Spacing.s2)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.inkMuted,
        )
        Text(text = stringResource(R.string.offline_chip), style = RunwayType.caption, color = colors.ink, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineChipPreview() {
    RunwayTheme { OfflineChip(Modifier.padding(Spacing.s4)) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0A26)
@Composable
private fun OfflineChipDarkPreview() {
    RunwayTheme(darkTheme = true) { OfflineChip(Modifier.padding(Spacing.s4)) }
}
