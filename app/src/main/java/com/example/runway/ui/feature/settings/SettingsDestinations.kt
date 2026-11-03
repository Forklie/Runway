package com.example.runway.ui.feature.settings

import kotlinx.serialization.Serializable

/**
 * Not a bottom-bar tab: it is opened from the gear icon on Home (C), so the bar hides while it is open.
 * C: `navController.navigate(SettingsDest)`.
 */
@Serializable data object SettingsDest
