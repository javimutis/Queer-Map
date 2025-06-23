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
    replaces = [DataSourceModule::class]
)
object TestSocialLoginDataSourceModule {

    @Provides
    @Singleton
    fun provideGoogleSignInDataSource(): GoogleSignInDataSource = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFacebookSignInDataSource(): FacebookSignInDataSource = mockk(relaxed = true)
}