package com.example.runway.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.RunwayType
import com.example.runway.ui.theme.Spacing

/**
 * Short task on the current screen (design system, Bottom sheet): surface fill, 32dp top corners,
 * drag handle, title-1, 16dp between blocks, one Large primary at the bottom that rides the keyboard.
 * Opens fully expanded; a sheet never opens another sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunwaySheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = RunwayTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RunwayShape.Sheet,
        containerColor = c.surface,
        scrimColor = c.scrim,
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.lineStrong) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.screenMargin)
                .padding(bottom = Spacing.s6),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            Text(title, style = RunwayType.title1, color = c.ink, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}
