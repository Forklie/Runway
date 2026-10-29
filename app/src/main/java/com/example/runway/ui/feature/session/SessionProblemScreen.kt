package com.example.runway.ui.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.runway.R
import com.example.runway.core.result.AppError
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.messageRes
import com.example.runway.ui.components.problemLockup
import com.example.runway.ui.theme.RunwayTheme

/** Full-screen "we couldn't open your account" with one action. Stateless. */
@Composable
fun SessionProblemScreen(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RunwayTheme.colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            lockup = problemLockup(),
            title = stringResource(R.string.error_title),
            body = stringResource(error.messageRes()),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionProblemOfflinePreview() {
    RunwayTheme { SessionProblemScreen(error = AppError.Network, onRetry = {}) }
}
