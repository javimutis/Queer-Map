package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.repository.AuthRepositoryImpl
import com.cursoandroid.queermap.data.source.remote.FirebaseAuthDataSource
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.cursoandroid.queermap.domain.usecase.LoginWithEmailUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideAuthRepository(firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore): AuthRepository =
        AuthRepositoryImpl(FirebaseAuthDataSource(firebaseAuth, firestore))

    @Provides
    fun provideLoginUseCase(authRepository: AuthRepository): LoginWithEmailUseCase =
        LoginWithEmailUseCase(authRepository)
}
