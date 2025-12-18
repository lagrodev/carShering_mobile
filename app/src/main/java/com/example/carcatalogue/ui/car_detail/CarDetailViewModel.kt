package com.example.carcatalogue.ui.car_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.CarDetailResponse
import com.example.carcatalogue.data.model.CreateContractRequest
import com.example.carcatalogue.data.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CarDetailUiState {
    object Loading : CarDetailUiState()
    data class Success(val car: CarDetailResponse) : CarDetailUiState()
    data class Error(val message: String) : CarDetailUiState()
}

class CarDetailViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = CarRepository(RetrofitClient.apiService)

    private val _uiState = MutableStateFlow<CarDetailUiState>(CarDetailUiState.Loading)
    val uiState: StateFlow<CarDetailUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(
        savedStateHandle.get<Boolean>(KEY_IS_FAVORITE) ?: false
    )
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _dateStart = MutableStateFlow(
        savedStateHandle.get<String?>(KEY_DATE_START)
    )
    val dateStart: StateFlow<String?> = _dateStart.asStateFlow()

    private val _dateEnd = MutableStateFlow(
        savedStateHandle.get<String?>(KEY_DATE_END)
    )
    val dateEnd: StateFlow<String?> = _dateEnd.asStateFlow()

    fun loadCarDetails(carId: Long) {
        viewModelScope.launch {
            _uiState.value = CarDetailUiState.Loading
            try {
                val response = repository.getCarDetail(carId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = CarDetailUiState.Success(response.body()!!)
                } else {
                    _uiState.value = CarDetailUiState.Error("Автомобиль не найден")
                }
            } catch (e: Exception) {
                _uiState.value = CarDetailUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    fun toggleFavorite(carId: Long) {
        viewModelScope.launch {
            try {
                val newFavoriteState = !_isFavorite.value
                
                if (newFavoriteState) {
                    RetrofitClient.apiService.addFavorite(carId)
                } else {
                    RetrofitClient.apiService.deleteFavorite(carId)
                }
                
                _isFavorite.value = newFavoriteState
                savedStateHandle[KEY_IS_FAVORITE] = newFavoriteState
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun setDateStart(date: String) {
        _dateStart.value = date
        savedStateHandle[KEY_DATE_START] = date
    }

    fun setDateEnd(date: String) {
        _dateEnd.value = date
        savedStateHandle[KEY_DATE_END] = date
    }

    fun calculateTotalCost(pricePerDay: Double): Double? {
        val start = _dateStart.value
        val end = _dateEnd.value
        
        if (start == null || end == null) return null
        
        // Простой расчет (в реальном приложении использовать правильный парсинг дат)
        // Предполагаем, что даты в формате yyyy-MM-dd
        try {
            val days = calculateDaysBetween(start, end)
            return days * pricePerDay
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateDaysBetween(start: String, end: String): Int {
        // Упрощенный расчет - в реальном приложении использовать java.time
        return 1 // Минимум 1 день
    }

    fun createContract(carId: Long, dailyRate: Double): CreateContractRequest? {
        val start = _dateStart.value
        val end = _dateEnd.value
        
        if (start == null || end == null) return null
        
            return CreateContractRequest(
                carId = carId,
                dataStart = start,
                dataEnd = end,
                dailyRate = dailyRate
        )
    }

    companion object {
        private const val KEY_IS_FAVORITE = "is_favorite"
        private const val KEY_DATE_START = "date_start"
        private const val KEY_DATE_END = "date_end"
    }
}
