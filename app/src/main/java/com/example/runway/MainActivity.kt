package com.example.runway

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.runway.ui.feature.quickadd.EXTRA_IMPORT_AMOUNT
import com.example.runway.ui.feature.quickadd.QuickAddViewModel
import com.example.runway.ui.feature.session.SessionState
import com.example.runway.ui.feature.session.SessionViewModel
import com.example.runway.ui.navigation.RunwayShell
import com.example.runway.ui.theme.RunwayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()
    private val quickAddViewModel: QuickAddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // must come before super.onCreate
        super.onCreate(savedInstanceState)
        // Keep the splash up until we know where to send the user (FR-02): no flash of the wrong screen.
        splash.setKeepOnScreenCondition { sessionViewModel.state.value is SessionState.Loading }
        enableEdgeToEdge()
        setContent {
            RunwayTheme {
                RunwayShell(sessionViewModel = sessionViewModel, quickAddViewModel = quickAddViewModel)
            }
        }
        if (savedInstanceState == null) importFrom(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importFrom(intent)
    }

    private fun importFrom(intent: Intent) {
        intent.getStringExtra(EXTRA_IMPORT_AMOUNT)?.let(quickAddViewModel::onImport)
        intent.removeExtra(EXTRA_IMPORT_AMOUNT) // reopening from recents must not ask again
    }
}
