package com.example.runway.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.runway.R
import com.example.runway.core.result.AppError

/** What went wrong plus one way forward, centred in the space it is given (Doc 1 section 9.5). */
@Composable
fun ErrorState(error: AppError, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            lockup = problemLockup(),
            title = stringResource(R.string.error_title),
            body = stringResource(error.messageRes()),
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }
}
