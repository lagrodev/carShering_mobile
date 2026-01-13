package com.example.carcatalogue.data.api

import android.content.Context
import com.example.carcatalogue.data.preferences.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

/**
 * Interceptor для автоматического обновления токенов при получении 401 ошибки
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    
    private val tokenManager by lazy { TokenManager(context) }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Пропускаем перехватчик для запросов аутентификации
        if (request.url.encodedPath.contains("/api/auth") ||
            request.url.encodedPath.contains("/api/registration") ||
            request.url.encodedPath.contains("/api/refresh")) {
            return chain.proceed(request)
        }
        
        // Добавляем токен если есть
        val token = runBlocking { 
            tokenManager.getToken().first() 
        }
        
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        
        // Выполняем запрос
        var response = chain.proceed(newRequest)
        
        // Если получили 401 - пробуем обновить токен
        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED && token != null) {
            response.close()
            
            // Пытаемся обновить токен
            val refreshed = runBlocking {
                refreshToken()
            }
            
            if (refreshed) {
                // Повторяем запрос с новым токеном
                val newToken = runBlocking { 
                    tokenManager.getToken().first() 
                }
                
                val retryRequest = if (newToken != null) {
                    request.newBuilder()
                        .addHeader("Authorization", "Bearer $newToken")
                        .build()
                } else {
                    request
                }
                
                response = chain.proceed(retryRequest)
            } else {
                // Не удалось обновить токен - очищаем и переходим на логин
                runBlocking { 
                    tokenManager.clearToken() 
                }
            }
        }
        
        return response
    }
    
    /**
     * Попытка обновления токена через /api/refresh
     * @return true если токен успешно обновлен, false в противном случае
     */
    private suspend fun refreshToken(): Boolean {
        return try {
            val response = RetrofitClient.apiService.refreshToken()
            
            if (response.isSuccessful) {
                // Токен обновится автоматически через cookies
                // Сервер установит новые cookies: access_token и refresh_token
                true
            } else {
                // Сервер вернул ошибку (401 - невалидный refresh token)
                // Очищаем токены
                tokenManager.clearToken()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
