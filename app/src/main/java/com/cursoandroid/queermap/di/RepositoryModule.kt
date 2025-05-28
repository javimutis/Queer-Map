package com.cursoandroid.queermap.di

import android.content.Context
import com.cursoandroid.queermap.data.repository.AuthRepositoryImpl
import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        remoteDataSource: AuthRemoteDataSource,
        sharedPreferencesDataSource: SharedPreferencesDataSource,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository =
        AuthRepositoryImpl(remoteDataSource, sharedPreferencesDataSource, firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideSharedPreferencesDataSource(@ApplicationContext context: Context): SharedPreferencesDataSource {
        return SharedPreferencesDataSource(context)
    }
}