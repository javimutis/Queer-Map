package com.cursoandroid.queermap.di

import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataSourceModule::class]
)
object TestSocialLoginDataSourceModule {

    @Provides
    fun provideGoogleSignInDataSource(): GoogleSignInDataSource = mockk(relaxed = true)

    @Provides
    fun provideFacebookSignInDataSource(): FacebookSignInDataSource = mockk(relaxed = true)
}