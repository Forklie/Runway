package com.example.runway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.runway.ui.feature.session.SessionState
import com.example.runway.ui.feature.session.SessionViewModel
import com.example.runway.ui.navigation.RunwayShell
import com.example.runway.ui.theme.RunwayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // must come before super.onCreate
        super.onCreate(savedInstanceState)
        // Keep the splash up until we know where to send the user (FR-02): no flash of the wrong screen.
        splash.setKeepOnScreenCondition { sessionViewModel.state.value is SessionState.Loading }
        enableEdgeToEdge()
        setContent {
            RunwayTheme {
                RunwayShell(sessionViewModel = sessionViewModel)
            }
        }
    }
}
