package com.cursoandroid.queermap.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInClient // Eliminar esta importación si ya no la necesitas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource // Esta importación puede ser eliminada o no, dependiendo de su uso
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // Este provide ya no es necesario aquí. GoogleSignInClient será gestionado internamente por GoogleSignInDataSourceImpl.
    // @Provides
    // @Singleton
    // fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
    //     return GoogleSignInDataSource(context).getGoogleSignInClient()
    // }
}