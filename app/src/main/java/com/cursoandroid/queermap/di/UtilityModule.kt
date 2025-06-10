// di/UtilityModule.kt
package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.util.EspressoIdlingResource
import com.cursoandroid.queermap.util.IdlingResourceProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilityModule {

    // Método @Binds abstracto
    @Singleton
    @Binds
    abstract fun bindIdlingResourceProvider(
        espressoIdlingResource: EspressoIdlingResource
    ): IdlingResourceProvider

    // Companion object para contener los métodos @Provides estáticos
    companion object {
        @Provides
        @Singleton
        @JvmStatic // <--- ¡Añadir esta anotación!
        fun provideEspressoIdlingResource(): EspressoIdlingResource {
            return EspressoIdlingResource // Devuelve la instancia del objeto
        }
    }
}