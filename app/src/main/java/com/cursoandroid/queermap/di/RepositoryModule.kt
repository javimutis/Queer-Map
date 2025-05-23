package com.cursoandroid.queermap.di

import android.content.Context
import com.cursoandroid.queermap.data.repository.AuthRepositoryImpl
import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.local.SharedPreferencesDataSource
import com.cursoandroid.queermap.domain.repository.AuthRepository
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
        sharedPreferencesDataSource: SharedPreferencesDataSource
    ): AuthRepository = AuthRepositoryImpl(remoteDataSource, sharedPreferencesDataSource)

    @Provides
    @Singleton
    fun provideSharedPreferencesDataSource(@ApplicationContext context: Context): SharedPreferencesDataSource {
        return SharedPreferencesDataSource(context)
    }
}