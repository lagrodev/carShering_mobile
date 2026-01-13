package com.example.carcatalogue.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.CarListItemResponse
import com.example.carcatalogue.data.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FavoritesUiState {
    object Loading : FavoritesUiState()
    data class Success(val cars: List<CarListItemResponse>) : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}

class FavoritesViewModel : ViewModel() {

    private val repository = CarRepository(RetrofitClient.apiService)

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var isLoading = false
    private val allCars = mutableListOf<CarListItemResponse>()

    init {
        loadFavorites()
    }

    fun loadFavorites(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            isLastPage = false
            allCars.clear()
        }

        if (isLoading || isLastPage) return
        isLoading = true

        viewModelScope.launch {
            try {
                if (currentPage == 0) {
                    _uiState.value = FavoritesUiState.Loading
                }

                val response = repository.getFavorites(page = currentPage, size = 20)

                if (response.isSuccessful) {
                    val pagedModel = response.body()
                    val cars = pagedModel?.content ?: emptyList()

                    allCars.addAll(cars)
                    _uiState.value = FavoritesUiState.Success(allCars.toList())

                    // Check if last page
                    val pageMetadata = pagedModel?.page
                    isLastPage = pageMetadata?.let {
                        (it.number + 1) >= it.totalPages
                    } ?: true

                    currentPage++
                } else {
                    _uiState.value = FavoritesUiState.Error("Ошибка загрузки избранного: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error("Ошибка сети: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun loadNextPage() {
        loadFavorites()
    }

    fun toggleFavorite(carId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                if (isFavorite) {
                    repository.removeFavorite(carId)
                    // Remove from local list
                    allCars.removeAll { it.id == carId }
                    _uiState.value = FavoritesUiState.Success(allCars.toList())
                } else {
                    repository.addFavorite(carId)
                }
            } catch (e: Exception) {
                // Handle error silently or show toast
            }
        }
    }
}
