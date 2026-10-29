package com.example.runway.data.di

import com.example.runway.data.repository.AccountRepositoryImpl
import com.example.runway.data.repository.UserProfileRepositoryImpl
import com.example.runway.domain.repository.AccountRepository
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
}
