package com.example.runway.ui.feature.auth

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.result.AppError
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.theme.RunwayTheme

@Composable
fun ResetPasswordRoute(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ResetPasswordScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSend = viewModel::onSendReset,
        onBack = onBack,
    )
}

/** Before the request: email field. After it: the neutral notice and a way back to sign in. */
@Composable
fun ResetPasswordScreen(
    state: AuthFormState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    AuthScaffold(
        lockup = listOf(
            LockupWord(stringResource(R.string.lockup_no), WordShape.Box),
            LockupWord(stringResource(R.string.lockup_worries), WordShape.Pill),
        ),
        title = stringResource(R.string.auth_reset_title),
        subtitle = stringResource(R.string.auth_reset_subtitle),
        primaryLabel = stringResource(
            if (state.resetRequested) R.string.auth_reset_back_to_login else R.string.auth_reset_action,
        ),
        onPrimary = if (state.resetRequested) onBack else onSend,
        submitting = state.submitting,
        onBack = onBack,
    ) {
        if (state.resetRequested) {
            AuthNotice(
                title = stringResource(R.string.auth_reset_sent_title),
                body = stringResource(R.string.auth_reset_sent_body),
            )
        } else {
            RunwayTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_email_label),
                placeholder = stringResource(R.string.auth_email_placeholder),
                errorText = state.emailErrorRes()?.let { stringResource(it) },
                enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    onSend()
                }),
            )
            state.bannerErrorRes()?.let { AuthErrorBanner(stringResource(it)) }
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ResetEmptyPreview() {
    RunwayTheme { ResetPasswordScreen(AuthFormState(), {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ResetOfflinePreview() {
    RunwayTheme {
        ResetPasswordScreen(AuthFormState(email = "kavindu@example.com", formError = AppError.Network), {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ResetSentPreview() {
    RunwayTheme { ResetPasswordScreen(AuthFormState(email = "kavindu@example.com", resetRequested = true), {}, {}, {}) }
}
