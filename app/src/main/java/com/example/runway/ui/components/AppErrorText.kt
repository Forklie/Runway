package com.example.runway.ui.components

import androidx.annotation.StringRes
import com.example.runway.R
import com.example.runway.core.result.AppError

/** The one place an AppError becomes user-facing text. Use with stringResource(). */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.PermissionDenied -> R.string.error_permission_denied
    AppError.NotFound -> R.string.error_not_found
    AppError.InvalidCredentials -> R.string.error_invalid_credentials
    AppError.EmailInUse -> R.string.error_email_in_use
    AppError.WeakPassword -> R.string.error_weak_password
    is AppError.Validation -> R.string.error_validation
    is AppError.Unknown -> R.string.error_unknown
}