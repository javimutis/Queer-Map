package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithGoogleUseCase
import com.cursoandroid.queermap.domain.usecase.auth.SendResetPasswordUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk

@Module
@TestInstallIn(
    components = [ViewModelComponent::class, SingletonComponent::class],
    replaces = [DataSourceModule::class, UseCaseModule::class]
)
object TestLoginModule {

    @Provides
    fun provideAuthRemoteDataSource(): AuthRemoteDataSource = mockk(relaxed = true)

    @Provides fun provideLoginWithEmailUseCase(): LoginWithEmailUseCase = mockk(relaxed = true)
    @Provides fun provideSendResetPasswordUseCase(): SendResetPasswordUseCase = mockk(relaxed = true)
    @Provides fun provideCreateUserUseCase(): CreateUserUseCase = mockk(relaxed = true)
    @Provides fun provideRegisterWithGoogleUseCase(): RegisterWithGoogleUseCase = mockk(relaxed = true)
    @Provides fun provideRegisterWithFacebookUseCase(): RegisterWithFacebookUseCase = mockk(relaxed = true)
}