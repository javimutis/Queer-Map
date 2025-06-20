package com.cursoandroid.queermap.di

import android.content.Context
import com.cursoandroid.queermap.data.source.remote.FacebookSignInDataSource
import com.cursoandroid.queermap.data.source.remote.GoogleSignInDataSource
import com.cursoandroid.queermap.fakes.FakeGoogleSignInDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideGoogleSignInDataSource(@ApplicationContext context: Context): GoogleSignInDataSource =
        FakeGoogleSignInDataSource(context)

    @Provides
    fun provideFacebookSignInDataSource(): FacebookSignInDataSource = mockk(relaxed = true)
}