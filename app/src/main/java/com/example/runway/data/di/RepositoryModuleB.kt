package com.example.runway.data.di

import com.example.runway.data.repository.AccountRepositoryImpl
import com.example.runway.data.repository.AuthRepositoryImpl
import com.example.runway.data.repository.GoalSettingsRepositoryImpl
import com.example.runway.data.repository.OnboardingRepositoryImpl
import com.example.runway.data.repository.RecurringRepositoryImpl
import com.example.runway.data.repository.TransactionRepositoryImpl
import com.example.runway.data.repository.UserProfileRepositoryImpl
import com.example.runway.domain.repository.AccountRepository
import com.example.runway.domain.repository.AuthRepository
import com.example.runway.domain.repository.GoalSettingsRepository
import com.example.runway.domain.repository.OnboardingRepository
import com.example.runway.domain.repository.RecurringRepository
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Member B's repository bindings. A and C keep their own module files (no merge conflicts). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModuleB {

    @Binds
    abstract fun accountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun userProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    abstract fun authRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun recurringRepository(impl: RecurringRepositoryImpl): RecurringRepository

    @Binds
    abstract fun onboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    abstract fun goalSettingsRepository(impl: GoalSettingsRepositoryImpl): GoalSettingsRepository

    @Binds
    abstract fun transactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
}
