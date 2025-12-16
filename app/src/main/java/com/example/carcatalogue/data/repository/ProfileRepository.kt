package com.example.carcatalogue.data.repository

import com.example.carcatalogue.data.api.ApiService
import com.example.carcatalogue.data.model.*
import retrofit2.Response

class ProfileRepository(private val apiService: ApiService) {
    
    suspend fun getProfile(): Response<UserResponse> {
        return apiService.getProfile()
    }
    
    suspend fun getMe(): Response<Unit> {
        return apiService.getMe()
    }
    
    suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phone: String?
    ): Response<Unit> {
        return apiService.updateProfile(UpdateProfileRequest(firstName, lastName, phone))
    }
    
    suspend fun deleteProfile(): Response<Unit> {
        return apiService.deleteProfile()
    }
    
    suspend fun changePassword(oldPassword: String, newPassword: String): Response<Unit> {
        return apiService.changePassword(ChangePasswordRequest(oldPassword, newPassword))
    }
    
    // Documents
    suspend fun getDocument(): Response<DocumentResponse> {
        return apiService.getDocument()
    }
    
    suspend fun createDocument(request: CreateDocumentRequest): Response<DocumentResponse> {
        return apiService.createDocument(request)
    }
    
    suspend fun updateDocument(request: UpdateDocumentRequest): Response<DocumentResponse> {
        return apiService.updateDocument(request)
    }
    
    suspend fun deleteDocument(): Response<Unit> {
        return apiService.deleteDocument()
    }
}
