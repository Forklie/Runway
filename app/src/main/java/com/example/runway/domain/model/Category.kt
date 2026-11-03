package com.example.runway.domain.model

/** BR-25. Committed happens regardless of mood (rent, gym); discretionary is chosen (food, rides). */
enum class CategoryKind { COMMITTED, DISCRETIONARY }

/** users/{uid}/categories/{id}. Seeded at sign-up (BR-26). Archived, never deleted (BR-28). */
data class Category(
    val id: String,
    val name: String,
    val iconKey: String, // Material icon name, e.g. "restaurant"
    val kind: CategoryKind,
    val sortOrder: Int,
    val archived: Boolean,
)
