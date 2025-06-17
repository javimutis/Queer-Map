package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.util.IdlingResourceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilityModule {

    @Binds
    @Singleton
    abstract fun bindIdlingResourceProvider(
        noOpIdlingResourceProvider: NoOpIdlingResourceProvider
    ): IdlingResourceProvider
}

@Singleton
class NoOpIdlingResourceProvider @Inject constructor() : IdlingResourceProvider {
    override fun increment() {
        // No-op
    }

    override fun decrement() {
        // No-op
    }
}