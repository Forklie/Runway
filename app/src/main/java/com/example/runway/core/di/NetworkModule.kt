package com.example.runway.core.di

import com.example.runway.core.network.ConnectivityObserver
import com.example.runway.core.network.NetworkConnectivityObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun connectivity(impl: NetworkConnectivityObserver): ConnectivityObserver
}