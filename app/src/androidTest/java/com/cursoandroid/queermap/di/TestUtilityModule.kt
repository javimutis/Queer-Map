// app/src/androidTest/java/com/cursoandroid/queermap/di/TestUtilityModule.kt
package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.util.EspressoIdlingResource
import com.cursoandroid.queermap.util.IdlingResourceProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn // <-- CORRECTO
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [UtilityModule::class] // <-- CORRECTO: Reemplaza tu módulo de producción
)
abstract class TestUtilityModule {

    @Binds
    @Singleton
    abstract fun bindIdlingResourceProvider(
        espressoIdlingResource: EspressoIdlingResource
    ): IdlingResourceProvider

    companion object {
        @Provides
        @Singleton
        fun provideEspressoIdlingResource(): EspressoIdlingResource {
            return EspressoIdlingResource
        }
    }
}