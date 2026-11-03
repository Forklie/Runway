package com.example.runway.data.di

import com.example.runway.data.repository.BalanceCheckinRepositoryImpl
import com.example.runway.data.repository.CryptoRepositoryImpl
import com.example.runway.data.repository.GoalRepositoryImpl
import com.example.runway.domain.repository.BalanceCheckinRepository
import com.example.runway.domain.repository.CryptoRepository
import com.example.runway.domain.repository.GoalRepository
import com.example.runway.domain.repository.RecurringRepository
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Member C's repository bindings. A and B keep their own module files (no merge conflicts). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModuleC {

    @Binds
    abstract fun goalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    abstract fun balanceCheckinRepository(impl: BalanceCheckinRepositoryImpl): BalanceCheckinRepository

    @Binds
    abstract fun cryptoRepository(impl: CryptoRepositoryImpl): CryptoRepository

    /**
     * B's RecurringRepository is bound once B's implementation lands. Until then Safe to Spend sees an
     * empty Optional and uses actual committed spending; afterwards it picks up the planned costs
     * with no change here (Dagger fills the Optional as soon as a binding exists).
     */
    @BindsOptionalOf
    abstract fun optionalRecurringRepository(): RecurringRepository
}
