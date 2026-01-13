package com.example.carcatalogue.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8082/"
    
    var authToken: String? = null
    private var authInterceptor: AuthInterceptor? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * Инициализация RetrofitClient с контекстом для AuthInterceptor
     */
    fun initialize(context: Context) {
        authInterceptor = AuthInterceptor(context)
        
        // Пересоздаём OkHttpClient с новым AuthInterceptor
        val newOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(InMemoryCookieJar())
            .addInterceptor(authInterceptor!!)
            .addInterceptor(loggingInterceptor)
            .build()
        
        val newRetrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(newOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        _apiService = newRetrofit.create(ApiService::class.java)
    }
    
    private var _apiService: ApiService? = null
    
    val apiService: ApiService
        get() = _apiService ?: createDefaultApiService()
    
    private fun createDefaultApiService(): ApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(InMemoryCookieJar())
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}