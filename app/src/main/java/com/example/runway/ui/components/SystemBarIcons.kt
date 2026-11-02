package com.example.runway.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.runway.ui.theme.RunwayTheme

/**
 * Light status-bar icons while the azul hero panel is under them (design system, Hero panel); the
 * theme's own choice again once it scrolls away or the screen leaves.
 */
@Composable
fun StatusBarIconsOnBrand(onBrand: Boolean) {
    val view = LocalView.current
    val dark = RunwayTheme.colors.isDark
    if (view.isInEditMode) return
    DisposableEffect(onBrand, dark) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !onBrand && !dark
        onDispose { controller?.isAppearanceLightStatusBars = !dark }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
