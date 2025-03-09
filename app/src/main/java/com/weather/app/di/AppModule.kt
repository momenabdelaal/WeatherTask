package com.weather.app.di

import android.app.Application
import android.content.Context
import com.weather.utils.error.ErrorHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideErrorHandler(context: Context): ErrorHandler {
        return ErrorHandler(context)
    }
}
