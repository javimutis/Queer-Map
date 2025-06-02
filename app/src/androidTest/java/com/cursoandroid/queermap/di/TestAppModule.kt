// app/src/androidTest/java/com/cursoandroid/queermap/di/TestAppModule.kt

package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.source.AuthRemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn // <-- Import TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

@Module
@TestInstallIn( // <-- Changed from @InstallIn
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class] // <-- Specify which module this one replaces
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(): AuthRemoteDataSource {
        return mockk(relaxed = true)
    }
}