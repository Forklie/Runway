package com.example.runway.ui.feature.auth

import androidx.annotation.StringRes
import com.example.runway.R
import com.example.runway.core.result.AppError
import com.example.runway.domain.validation.EmailIssue
import com.example.runway.domain.validation.NameIssue
import com.example.runway.domain.validation.PasswordIssue
import com.example.runway.ui.components.messageRes

/** Everything the three auth screens show. Input is kept on every error (AT-20). */
data class AuthFormState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameIssue: NameIssue? = null,
    val emailIssue: EmailIssue? = null,
    val passwordIssue: PasswordIssue? = null,
    /** An error that came back from Firebase (wrong password, offline, duplicate email...). */
    val formError: AppError? = null,
    val submitting: Boolean = false,
    /** Reset screen: the request was made. The message is neutral on purpose. */
    val resetRequested: Boolean = false,
)

// ---- AppError / issue -> string. The only place auth text is chosen. ----

@StringRes
fun EmailIssue.messageRes(): Int = when (this) {
    EmailIssue.Empty -> R.string.auth_error_email_empty
    EmailIssue.Invalid -> R.string.auth_error_email_invalid
}

@StringRes
fun PasswordIssue.messageRes(): Int = when (this) {
    PasswordIssue.Empty -> R.string.auth_error_password_empty
    PasswordIssue.TooShort -> R.string.error_weak_password
}

@StringRes
fun NameIssue.messageRes(): Int = R.string.auth_error_name_empty

/** Under the email field: our own check, or "this email is already used". */
@StringRes
fun AuthFormState.emailErrorRes(): Int? =
    emailIssue?.messageRes() ?: if (formError == AppError.EmailInUse) R.string.error_email_in_use else null

/** Under the password field: our own check, or Firebase calling the password weak. */
@StringRes
fun AuthFormState.passwordErrorRes(): Int? =
    passwordIssue?.messageRes() ?: if (formError == AppError.WeakPassword) R.string.error_weak_password else null

/** Everything else (wrong password, offline, unknown) goes in the banner above the button. */
@StringRes
fun AuthFormState.bannerErrorRes(): Int? =
    formError?.takeUnless { it == AppError.EmailInUse || it == AppError.WeakPassword }?.messageRes()
