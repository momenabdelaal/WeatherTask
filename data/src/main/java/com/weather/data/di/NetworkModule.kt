package com.weather.data.di

import com.weather.data.api.WeatherApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                var retryCount = 0
                val maxRetries = 3
                var response: okhttp3.Response? = null
                var lastException: Exception? = null

                while (retryCount < maxRetries && response == null) {
                    try {
                        val request = chain.request()
                        response = chain.proceed(request)
                        
                        if (!response.isSuccessful) {
                            response.close()
                            throw HttpException(retrofit2.Response.error<Any>(response.code, response.body!!))
                        }
                    } catch (e: Exception) {
                        lastException = e
                        if (retryCount == maxRetries - 1) {
                            throw when (e) {
                                is UnknownHostException -> IOException("تأكد من اتصالك بالإنترنت")
                                is SocketTimeoutException -> IOException("انتهت مهلة الاتصال")
                                is HttpException -> IOException("حدث خطأ في الاتصال بالخادم")
                                else -> e
                            }
                        }
                        retryCount++
                        Thread.sleep((1000 * retryCount).toLong()) // Exponential backoff
                    }
                }
                response!!
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApi(okHttpClient: OkHttpClient): WeatherApi {
        return Retrofit.Builder()
            .baseUrl(WeatherApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
}
