package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.repository.AuthRepositoryImpl
import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        remoteDataSource: AuthRemoteDataSource
    ): AuthRepository = AuthRepositoryImpl(remoteDataSource)
}
