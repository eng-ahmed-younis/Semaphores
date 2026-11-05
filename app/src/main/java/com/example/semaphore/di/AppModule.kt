package com.example.semaphore.di

import com.example.semaphore.data.remote.CrmApi
import com.example.semaphore.data.remote.CrmApiImpl
import com.example.semaphore.data.repository.ContactRepositoryImpl
import com.example.semaphore.domain.repository.ContactRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    companion object {
        @Provides
        @Singleton
        fun provideCrmApi(): CrmApi {
            return CrmApiImpl()
        }
    }
}
