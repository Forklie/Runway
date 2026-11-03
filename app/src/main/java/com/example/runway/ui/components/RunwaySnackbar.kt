package com.example.runway.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runway.ui.theme.Dimens
import com.example.runway.ui.theme.RunwayShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.Spacing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Inverse-surface snackbar, radius-md, 12dp above whatever it is anchored to, 20dp from the sides. */
@Composable
fun RunwaySnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    val c = RunwayTheme.colors
    SnackbarHost(state, modifier.padding(horizontal = Dimens.screenMargin, vertical = Spacing.s3)) { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.widthIn(max = 344.dp),
            shape = RunwayShape.Field,
            containerColor = c.inverseSurface,
            contentColor = c.onInverse,
            actionColor = c.inverseAction,
        )
    }
}

/**
 * Undo stays up for 6 seconds (design system, Snackbar): Material's Short is too short for undo and
 * Long is 10 s. One snackbar at a time: a new one replaces the old one.
 */
suspend fun SnackbarHostState.showUndo(message: String, actionLabel: String): SnackbarResult = coroutineScope {
    currentSnackbarData?.dismiss()
    val timer = launch {
        delay(UNDO_MILLIS)
        currentSnackbarData?.dismiss()
    }
    showSnackbar(message, actionLabel, duration = SnackbarDuration.Indefinite).also { timer.cancel() }
}

private const val UNDO_MILLIS = 6_000L
