package com.example.runway.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.runway.R
import com.example.runway.ui.components.EmptyState
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.WordShape

/** Temporary. Each owner deletes their placeholder line in AppNavHost when their screen lands. */
@Composable
fun PlaceholderScreen(@StringRes titleRes: Int) {
    EmptyState(
        lockup = listOf(
            LockupWord(stringResource(R.string.lockup_coming), WordShape.Box),
            LockupWord(stringResource(R.string.lockup_soon), WordShape.Pill),
        ),
        title = stringResource(titleRes),
        body = stringResource(R.string.placeholder_body),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    )
}