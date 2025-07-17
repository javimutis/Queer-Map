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
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton
// No longer need javax.inject.Inject here for the class itself

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataSourceModule::class, UseCaseModule::class]
)
// --- CHANGE IS HERE ---
// Convert to an abstract class, and put @Provides methods inside a companion object.
// This is a common and robust pattern for Hilt modules.
abstract class TestLoginModule { // Changed from 'class TestLoginModule @Inject constructor()'

    companion object { // Add a companion object
        @Provides
        @Singleton
        fun provideGoogleSignInDataSource(): GoogleSignInDataSource = mockk(relaxed = true)

        @Provides
        @Singleton
        fun provideFacebookSignInDataSource(): FacebookSignInDataSource = mockk(relaxed = true)

        @Provides
        fun provideAuthRemoteDataSource(): AuthRemoteDataSource = mockk(relaxed = true)

        @Provides fun provideLoginWithEmailUseCase(): LoginWithEmailUseCase = mockk(relaxed = true)
        @Provides fun provideSendResetPasswordUseCase(): SendResetPasswordUseCase = mockk(relaxed = true)
        @Provides fun provideCreateUserUseCase(): CreateUserUseCase = mockk(relaxed = true)
        @Provides fun provideRegisterWithGoogleUseCase(): RegisterWithGoogleUseCase = mockk(relaxed = true)
        @Provides fun provideRegisterWithFacebookUseCase(): RegisterWithFacebookUseCase = mockk(relaxed = true)
    }
}
