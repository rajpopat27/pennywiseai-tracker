package com.fintrace.app.di

import com.fintrace.app.data.repository.AppPreferencesRepository
import com.fintrace.app.data.repository.AppPreferencesRepositoryImpl
import com.fintrace.app.data.repository.BudgetRepository
import com.fintrace.app.data.repository.BudgetRepositoryImpl
import com.fintrace.app.data.repository.HiddenAccountsRepository
import com.fintrace.app.data.repository.HiddenAccountsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindHiddenAccountsRepository(
        impl: HiddenAccountsRepositoryImpl
    ): HiddenAccountsRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        impl: AppPreferencesRepositoryImpl
    ): AppPreferencesRepository
}
