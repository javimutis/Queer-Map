package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.auth.CreateUserUseCase
import com.cursoandroid.queermap.domain.usecase.auth.LoginWithEmailUseCase
import com.cursoandroid.queermap.domain.usecase.auth.SendResetPasswordUseCase
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithGoogleUseCase // Nuevo
import com.cursoandroid.queermap.domain.usecase.auth.RegisterWithFacebookUseCase // Nuevo
import com.google.firebase.firestore.FirebaseFirestore // Necesario para los nuevos UseCases
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
    fun provideSendResetPasswordUseCase(authRepository: AuthRepository): SendResetPasswordUseCase =
        SendResetPasswordUseCase(authRepository)

    @Provides
    fun provideCreateUserUseCase(authRepository: AuthRepository): CreateUserUseCase =
        CreateUserUseCase(authRepository)

    @Provides
    fun provideRegisterWithGoogleUseCase(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ): RegisterWithGoogleUseCase =
        RegisterWithGoogleUseCase(authRepository, firestore)

    @Provides
    fun provideRegisterWithFacebookUseCase(
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ): RegisterWithFacebookUseCase =
        RegisterWithFacebookUseCase(authRepository, firestore)
}