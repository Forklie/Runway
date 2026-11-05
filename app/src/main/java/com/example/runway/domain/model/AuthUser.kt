package com.example.runway.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
)