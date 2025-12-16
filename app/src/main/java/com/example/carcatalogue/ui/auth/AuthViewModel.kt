package com.example.carcatalogue.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.Result
import com.example.carcatalogue.data.model.UserResponse
import com.example.carcatalogue.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val repository = AuthRepository(RetrofitClient.apiService)
    
    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult
    
    private val _registerResult = MutableLiveData<Result<UserResponse>>()
    val registerResult: LiveData<Result<UserResponse>> = _registerResult
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = Result.Loading
            try {
                val response = repository.authenticate(username, password)
                if (response.isSuccessful) {
                    _loginResult.value = Result.Success(Unit)
                } else {
                    _loginResult.value = Result.Error(
                        response.errorBody()?.string() ?: "Ошибка авторизации"
                    )
                }
            } catch (e: Exception) {
                _loginResult.value = Result.Error(e.message ?: "Ошибка сети")
            }
        }
    }
    
    fun register(login: String, password: String, lastName: String, email: String) {
        viewModelScope.launch {
            _registerResult.value = Result.Loading
            try {
                val response = repository.register(login, password, lastName, email)
                if (response.isSuccessful && response.body() != null) {
                    _registerResult.value = Result.Success(response.body()!!)
                } else {
                    _registerResult.value = Result.Error(
                        response.errorBody()?.string() ?: "Ошибка регистрации"
                    )
                }
            } catch (e: Exception) {
                _registerResult.value = Result.Error(e.message ?: "Ошибка сети")
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
                RetrofitClient.authToken = null
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}
