package com.cursoandroid.queermap.di

import android.content.Context
import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.cursoandroid.queermap.data.source.remote.FirebaseAuthDataSource
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSourceImpl
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSourceImpl
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
object DataSourceModule {

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRemoteDataSource =
        FirebaseAuthDataSource(firebaseAuth, firestore)

    @Provides
    @Singleton
    fun provideGoogleSignInDataSource(@ApplicationContext context: Context): GoogleSignInDataSource {
        return GoogleSignInDataSourceImpl(context)
    }

    @Provides
    @Singleton
    fun provideFacebookSignInDataSource(): FacebookSignInDataSource {
        return FacebookSignInDataSourceImpl()
    }
}