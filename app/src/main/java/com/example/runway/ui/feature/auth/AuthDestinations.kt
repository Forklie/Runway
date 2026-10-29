package com.example.runway.ui.feature.auth

import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

// Type-safe routes of the auth graph. Nothing carries arguments: the form state lives in the ViewModel.
@Serializable data object LoginDest
@Serializable data object RegisterDest
@Serializable data object ResetPasswordDest

/** RunwayShell uses this to hide the bottom bar and Quick Add on auth screens. */
val authDestinations: List<KClass<*>> = listOf(
    LoginDest::class,
    RegisterDest::class,
    ResetPasswordDest::class,
)
