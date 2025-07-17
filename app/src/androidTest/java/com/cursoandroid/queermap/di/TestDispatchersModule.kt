package com.cursoandroid.queermap.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object TestDispatchersModule {

 @OptIn(ExperimentalCoroutinesApi::class)
 @Provides
 @Singleton
 fun provideTestCoroutineScheduler(): TestCoroutineScheduler = TestCoroutineScheduler()


 @OptIn(ExperimentalCoroutinesApi::class)
 @Provides
 @Singleton
 @IoDispatcher
 fun provideIoDispatcher(scheduler: TestCoroutineScheduler): CoroutineDispatcher = StandardTestDispatcher(scheduler)

 @OptIn(ExperimentalCoroutinesApi::class)
 @Provides
 @Singleton
 @MainDispatcher
 fun provideMainDispatcher(scheduler: TestCoroutineScheduler): CoroutineDispatcher = StandardTestDispatcher(scheduler)

 @OptIn(ExperimentalCoroutinesApi::class)
 @Provides
 @Singleton
 @DefaultDispatcher
 fun provideDefaultDispatcher(scheduler: TestCoroutineScheduler): CoroutineDispatcher = StandardTestDispatcher(scheduler)
}