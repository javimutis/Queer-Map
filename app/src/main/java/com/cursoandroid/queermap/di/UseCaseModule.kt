package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.LoginWithFacebookUseCase
import com.cursoandroid.queermap.domain.usecase.LoginWithGoogleUseCase
import com.cursoandroid.queermap.domain.usecase.SendResetPasswordUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideLoginWithEmailUseCase(authRepository: AuthRepository): LoginWithEmailUseCase =
        LoginWithEmailUseCase(authRepository)

    @Provides
    fun provideLoginWithFacebookUseCase(authRepository: AuthRepository): LoginWithFacebookUseCase =
        LoginWithFacebookUseCase(authRepository)

    @Provides
    fun provideLoginWithGoogleUseCase(authRepository: AuthRepository): LoginWithGoogleUseCase =
        LoginWithGoogleUseCase(authRepository)

    @Provides
    fun provideSendResetPasswordUseCase(authRepository: AuthRepository): SendResetPasswordUseCase =
        SendResetPasswordUseCase(authRepository)
}
