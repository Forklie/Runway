package com.example.runway.data.di

import com.example.runway.data.repository.CategoryRepositoryImpl
import com.example.runway.data.repository.IncomeSourceRepositoryImpl
import com.example.runway.data.repository.TransactionRepositoryImpl
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import com.example.runway.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Member A's repository bindings (added by C as A's buddy, see docs/DECISIONS.md). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModuleA {

    @Binds
    abstract fun transactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun categoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun incomeSourceRepository(impl: IncomeSourceRepositoryImpl): IncomeSourceRepository
}
