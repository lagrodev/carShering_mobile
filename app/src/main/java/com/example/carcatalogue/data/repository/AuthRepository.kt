package com.example.carcatalogue.data.repository

import com.example.carcatalogue.data.api.ApiService
import com.example.carcatalogue.data.model.*
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {
    
    suspend fun authenticate(username: String, password: String): Response<Unit> {
        return apiService.login(AuthRequest(username, password))
    }
    
    suspend fun register(
        login: String,
        password: String,
        lastName: String,
        email: String
    ): Response<UserResponse> {
        return apiService.register(RegistrationRequest(login, password, lastName, email))
    }
    
    suspend fun logout(): Response<Unit> {
        return apiService.logout()
    }
}
