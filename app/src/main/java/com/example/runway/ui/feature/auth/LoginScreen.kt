package com.example.runway.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runway.R
import com.example.runway.core.result.AppError
import com.example.runway.ui.components.ButtonSize
import com.example.runway.ui.components.ButtonStyle
import com.example.runway.ui.components.LockupWord
import com.example.runway.ui.components.RunwayButton
import com.example.runway.ui.components.RunwayTextField
import com.example.runway.ui.components.WordShape
import com.example.runway.ui.theme.RunwayTheme
import com.example.runway.ui.theme.Spacing

/** Stateful wrapper: the only part that touches Hilt. */
@Composable
fun LoginRoute(
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignIn = viewModel::onSignIn,
        onForgotPassword = onForgotPassword,
        onCreateAccount = onCreateAccount,
    )
}

/** Stateless: plain state and lambdas, so it previews and tests without Hilt or Firebase. */
@Composable
fun LoginScreen(
    state: AuthFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val focus = LocalFocusManager.current
    AuthScaffold(
        lockup = listOf(
            LockupWord(stringResource(R.string.lockup_welcome), WordShape.Box),
            LockupWord(stringResource(R.string.lockup_back), WordShape.Pill),
        ),
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle),
        primaryLabel = stringResource(R.string.auth_login_action),
        onPrimary = onSignIn,
        submitting = state.submitting,
        footer = {
            RunwayButton(
                text = stringResource(R.string.auth_login_to_register),
                onClick = onCreateAccount,
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Medium,
                enabled = !state.submitting,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            RunwayTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_email_label),
                placeholder = stringResource(R.string.auth_email_placeholder),
                errorText = state.emailErrorRes()?.let { stringResource(it) },
                enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            )
            RunwayTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_password_label),
                errorText = state.passwordErrorRes()?.let { stringResource(it) },
                enabled = !state.submitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    onSignIn()
                }),
            )
        }
        state.bannerErrorRes()?.let { AuthErrorBanner(stringResource(it)) }
        RunwayButton(
            text = stringResource(R.string.auth_forgot_action),
            onClick = onForgotPassword,
            style = ButtonStyle.Tertiary,
            size = ButtonSize.Small,
            enabled = !state.submitting,
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun LoginEmptyPreview() {
    RunwayTheme { LoginScreen(AuthFormState(), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun LoginWrongPasswordPreview() {
    RunwayTheme {
        LoginScreen(
            AuthFormState(email = "kavindu@example.com", password = "secret1", formError = AppError.InvalidCredentials),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun LoginSubmittingPreview() {
    RunwayTheme {
        LoginScreen(AuthFormState(email = "kavindu@example.com", password = "secret1", submitting = true), {}, {}, {}, {}, {})
    }
}
