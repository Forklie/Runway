package com.example.runway.data.di

import com.example.runway.data.repository.CategoryRepositoryImpl
import com.example.runway.data.repository.FreelanceRepositoryImpl
import com.example.runway.data.repository.IncomeSourceRepositoryImpl
import com.example.runway.domain.repository.CategoryRepository
import com.example.runway.domain.repository.FreelanceRepository
import com.example.runway.domain.repository.IncomeSourceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Member A's repository bindings (see docs/DECISIONS.md).
 * TransactionRepository is bound in RepositoryModuleB, which added it first for GenerateRecurring:
 * binding it here too would be a duplicate binding.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModuleA {

    @Binds
    abstract fun categoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun incomeSourceRepository(impl: IncomeSourceRepositoryImpl): IncomeSourceRepository

    @Binds
    abstract fun freelanceRepository(impl: FreelanceRepositoryImpl): FreelanceRepository
}
