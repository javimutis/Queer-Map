package com.cursoandroid.queermap.di

import android.content.Context // Importa Context si es necesario para otros mocks
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext // Importa ApplicationContext si es necesario
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk // **Asegúrate de tener mockk importado**
import javax.inject.Singleton // Necesario para el scope Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataSourceModule::class] // Asumiendo que DataSourceModule es tu módulo real que provee estas dependencias
)
object TestSocialLoginDataSourceModule {

    // **NUEVA MODIFICACIÓN:** Provee un mockk de GoogleSignInDataSource para que Hilt pueda inyectarlo globalmente
    @Provides
    @Singleton // Asegúrate de que sea Singleton si la implementación real lo es, para consistencia
    fun provideGoogleSignInDataSource(): GoogleSignInDataSource = mockk(relaxed = true)

    @Provides
    @Singleton // Asegúrate de que sea Singleton si la implementación real lo es
    fun provideFacebookSignInDataSource(): FacebookSignInDataSource = mockk(relaxed = true)
}