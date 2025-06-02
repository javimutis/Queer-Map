package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    // Reemplazamos el DataSourceModule real por nuestro FakeModule en los tests.
    // Esto asegura que Hilt inyecte nuestros mocks en lugar de las implementaciones reales.
    replaces = [DataSourceModule::class]
)
object FakeModule {

    // Proveemos un mock de GoogleSignInDataSource para que sea inyectado en los tests.
    // 'relaxed = true' permite que los métodos mockeados devuelvan valores por defecto
    // sin necesidad de un 'every' explícito para cada llamada que no nos interese probar.
    @Singleton
    @Provides
    fun provideFakeGoogleSignInDataSource(): GoogleSignInDataSource {
        return mockk(relaxed = true)
    }

    // Proveemos un mock de FacebookSignInDataSource para que sea inyectado en los tests.
    @Singleton
    @Provides
    fun provideFakeFacebookSignInDataSource(): FacebookSignInDataSource {
        return mockk(relaxed = true)
    }
}