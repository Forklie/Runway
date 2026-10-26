package com.example.runway.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.runway.R
import com.example.runway.ui.theme.RunwayTheme

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val colors = RunwayTheme.colors
    val description = stringResource(R.string.common_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = colors.accent, trackColor = colors.accentSoft, strokeWidth = 4.dp)
    }
}