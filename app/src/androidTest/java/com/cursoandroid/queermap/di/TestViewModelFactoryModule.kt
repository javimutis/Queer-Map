package com.cursoandroid.queermap.di

import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ViewModelFactoryModule::class]
)

object TestViewModelFactoryModule {

    @Singleton
    @Provides
    fun provideFakeViewModelFactory(): ViewModelProvider.Factory {
        return ViewModelProvider.NewInstanceFactory()
    }

}