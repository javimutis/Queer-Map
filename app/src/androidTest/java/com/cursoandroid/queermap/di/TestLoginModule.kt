package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk


@Module
@TestInstallIn(
    components = [ViewModelComponent::class, SingletonComponent::class],
    // ¡AHORA SÍ! Reemplaza los módulos de producción confirmados
    replaces = [DataSourceModule::class, UseCaseModule::class]
)
object TestLoginModule {

    @Provides
    fun provideGoogleSignInDataSource(): GoogleSignInDataSource = mockk(relaxed = true)

    @Provides
    fun provideFacebookSignInDataSource(): FacebookSignInDataSource = mockk(relaxed = true)

    @Provides
    fun provideAuthRemoteDataSource(): AuthRemoteDataSource = mockk(relaxed = true)

    @Provides
    fun provideCreateUserUseCase(): CreateUserUseCase = mockk(relaxed = true)
}