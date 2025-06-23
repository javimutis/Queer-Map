// src/main/java/com/cursoandroid/queermap/di/ViewModelFactoryModule.kt
package com.cursoandroid.queermap.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@dagger.hilt.InstallIn(SingletonComponent::class)
object ViewModelFactoryModule {

    @Singleton
    @Provides
    fun provideDefaultViewModelFactory(): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                throw UnsupportedOperationException("This factory is only used as placeholder.")
            }
        }
    }
}
