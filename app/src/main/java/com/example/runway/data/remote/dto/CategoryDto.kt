package com.example.runway.data.remote.dto

/** users/{uid}/categories/{id}. Same field names as data/seed/CategorySeed.kt writes at sign-up. */
data class CategoryDto(
    var name: String = "",
    var iconKey: String = "",
    var kind: String = "",
    var sortOrder: Int = 0,
    var archived: Boolean = false,
)
