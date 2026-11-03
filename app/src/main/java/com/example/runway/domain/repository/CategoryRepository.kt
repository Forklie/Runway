package com.example.runway.domain.repository

import com.example.runway.core.result.AppResult
import com.example.runway.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>> // non-archived, by sortOrder

    suspend fun add(category: Category): AppResult<Unit>
    suspend fun update(category: Category): AppResult<Unit>
    suspend fun archive(id: String, archived: Boolean = true): AppResult<Unit> // never deleted (BR-28)
}
