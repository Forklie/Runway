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
import androidx.compose.ui.text.input.KeyboardCapitalization
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

@Composable
fun RegisterRoute(
    onBack: () -> Unit,
    onHaveAccount: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onGoogle = rememberGoogleSignIn(viewModel)
    RegisterScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRegister = viewModel::onRegister,
        onBack = onBack,
        onHaveAccount = onHaveAccount,
        onGoogle = onGoogle,
    )
}

@Composable
fun RegisterScreen(
    state: AuthFormState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onHaveAccount: () -> Unit,
    onGoogle: () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    AuthScaffold(
        lockup = listOf(
            LockupWord(stringResource(R.string.lockup_hi), WordShape.Box),
            LockupWord(stringResource(R.string.lockup_there), WordShape.Pill),
        ),
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle),
        primaryLabel = stringResource(R.string.auth_register_action),
        onPrimary = onRegister,
        submitting = state.submitting,
        onBack = onBack,
        footer = {
            RunwayButton(
                text = stringResource(R.string.auth_google_action),
                onClick = onGoogle,
                style = ButtonStyle.Secondary,
                size = ButtonSize.Large,
                fillWidth = true,
                enabled = !state.submitting,
            )
            RunwayButton(
                text = stringResource(R.string.auth_register_to_login),
                onClick = onHaveAccount,
                style = ButtonStyle.Tertiary,
                size = ButtonSize.Medium,
                enabled = !state.submitting,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            RunwayTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.auth_name_label),
                errorText = state.nameIssue?.let { stringResource(it.messageRes()) },
                enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            )
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
                helperText = stringResource(R.string.auth_password_helper),
                errorText = state.passwordErrorRes()?.let { stringResource(it) },
                enabled = !state.submitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    onRegister()
                }),
            )
        }
        state.bannerErrorRes()?.let { AuthErrorBanner(stringResource(it)) }
    }
}

@Preview(showBackground = true, heightDp = 820)
@Composable
private fun RegisterEmptyPreview() {
    RunwayTheme { RegisterScreen(AuthFormState(), {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 820)
@Composable
private fun RegisterDuplicateEmailPreview() {
    RunwayTheme {
        RegisterScreen(
            AuthFormState(name = "Kavindu", email = "kavindu@example.com", password = "secret1", formError = AppError.EmailInUse),
            {}, {}, {}, {}, {}, {},
        )
    }
}
