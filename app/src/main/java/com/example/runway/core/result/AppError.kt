package com.example.runway.core.result

sealed interface AppError {
    data object Network : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object InvalidCredentials : AppError
    data object EmailInUse : AppError
    data object WeakPassword : AppError
    data class Validation(val field: String, val reason: String) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}