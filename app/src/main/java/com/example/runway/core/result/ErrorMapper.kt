package com.example.runway.core.result

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Data layer only. ViewModels and UI see AppError, never Firebase types. */
fun Throwable.toAppError(): AppError = when (this) {
    is FirebaseNetworkException -> AppError.Network
    // WeakPassword extends InvalidCredentials, so it must come first.
    is FirebaseAuthWeakPasswordException -> AppError.WeakPassword
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> AppError.InvalidCredentials
    is FirebaseAuthUserCollisionException -> AppError.EmailInUse
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.PermissionDenied
        FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network
        FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound
        else -> AppError.Unknown(this)
    }
    else -> AppError.Unknown(this)
}