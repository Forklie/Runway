package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.CategoryDto
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

/** An unknown kind reads as DISCRETIONARY: it never inflates Safe to Spend's committed share. */
fun CategoryDto.toDomain(id: String): Category = Category(
    id = id,
    name = name.ifBlank { "Other" },
    iconKey = iconKey.ifBlank { "more_horiz" },
    kind = kind.toEnumOr(CategoryKind.DISCRETIONARY),
    sortOrder = sortOrder,
    archived = archived,
)

fun Category.toDto(): CategoryDto = CategoryDto(
    name = name,
    iconKey = iconKey,
    kind = kind.name,
    sortOrder = sortOrder,
    archived = archived,
)

fun DocumentSnapshot.toCategoryOrNull(): Category? =
    try {
        toObject(CategoryDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed category %s", id)
        null
    }
