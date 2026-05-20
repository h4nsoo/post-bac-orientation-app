package com.example.orientation_app.data.di

import com.example.orientation_app.data.remote.GeminiServiceImpl
import com.example.orientation_app.data.repository.FiliereRepositoryImpl
import com.example.orientation_app.data.repository.UserRepositoryImpl
import com.example.orientation_app.domain.repository.FiliereRepository
import com.example.orientation_app.domain.repository.IAiService
import com.example.orientation_app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every domain repository interface to its Room-backed implementation.
 * To swap an implementation (e.g. for testing), replace the binding here — no
 * ViewModel code needs to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFiliereRepository(impl: FiliereRepositoryImpl): FiliereRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindAiService(impl: GeminiServiceImpl): IAiService
}
