package com.example.carcatalogue.ui.catalogue

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carcatalogue.data.api.RetrofitClient
import com.example.carcatalogue.data.model.CarListItemResponse
import com.example.carcatalogue.data.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Date

@Parcelize
data class CarFilters(
    val brands: List<String> = emptyList(),
    val model: String? = null,
    val bodyType: String? = null,
    val carClasses: List<String> = emptyList(),
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val minCell: Int? = null,
    val maxCell: Int? = null,
    val dateStart: @RawValue Date? = null,
    val dateEnd: @RawValue Date? = null
) : Parcelable

@Parcelize
data class CatalogueFilters(
    val brands: List<String> = emptyList(),
    val model: String? = null,
    val bodyType: String? = null,
    val carClasses: List<String> = emptyList(),
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val searchQuery: String? = null
) : Parcelable

sealed class CatalogueUiState {
    object Loading : CatalogueUiState()
    data class Success(val cars: List<CarListItemResponse>) : CatalogueUiState()
    data class Error(val message: String) : CatalogueUiState()
}

class CatalogueViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = CarRepository(RetrofitClient.apiService)

    // Сохраняем фильтры в SavedStateHandle для сохранения при повороте экрана
    private val _filters = MutableStateFlow(
        savedStateHandle.get<CatalogueFilters>(KEY_FILTERS) ?: CatalogueFilters()
    )
    val filters: StateFlow<CatalogueFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<CatalogueUiState>(CatalogueUiState.Loading)
    val uiState: StateFlow<CatalogueUiState> = _uiState.asStateFlow()

    private val _brands = MutableStateFlow<List<String>>(emptyList())
    val brands: StateFlow<List<String>> = _brands.asStateFlow()

    private val _models = MutableStateFlow<List<String>>(emptyList())
    val models: StateFlow<List<String>> = _models.asStateFlow()

    private val _bodyTypes = MutableStateFlow<List<String>>(emptyList())
    val bodyTypes: StateFlow<List<String>> = _bodyTypes.asStateFlow()

    private val _carClasses = MutableStateFlow<List<String>>(emptyList())
    val carClasses: StateFlow<List<String>> = _carClasses.asStateFlow()

    private val _minPrice = MutableStateFlow(500.0)
    val minPrice: StateFlow<Double> = _minPrice.asStateFlow()

    private val _maxPrice = MutableStateFlow(20000.0)
    val maxPrice: StateFlow<Double> = _maxPrice.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false
    private val allCars = mutableListOf<CarListItemResponse>()

    init {
        loadCars()
        loadFilterOptions()
    }

    fun loadCars(resetPage: Boolean = true) {
        if (resetPage) {
            currentPage = 0
            isLastPage = false
            allCars.clear()
        }

        if (isLoadingMore || isLastPage) return

        viewModelScope.launch {
            if (currentPage == 0) {
                _uiState.value = CatalogueUiState.Loading
            }
            isLoadingMore = true

            try {
                val currentFilters = _filters.value
                val response = repository.getCatalogue(
                    brands = currentFilters.brands.takeIf { it.isNotEmpty() },
                    model = currentFilters.model,
                    minYear = currentFilters.minYear,
                    maxYear = currentFilters.maxYear,
                    bodyType = currentFilters.bodyType,
                    carClasses = currentFilters.carClasses.takeIf { it.isNotEmpty() },
                    dateStart = currentFilters.dateStart,
                    dateEnd = currentFilters.dateEnd,
                    minCell = currentFilters.minPrice,
                    maxCell = currentFilters.maxPrice,
                    page = currentPage,
                    size = 20
                )

                if (response.isSuccessful) {
                    val pagedData = response.body()
                    val newCars = pagedData?.content ?: emptyList()
                    
                    allCars.addAll(newCars)
                    isLastPage = pagedData?.page?.number?.let { it >= pagedData.page.totalPages - 1 } ?: true
                    currentPage++

                    _uiState.value = CatalogueUiState.Success(allCars.toList())
                } else {
                    _uiState.value = CatalogueUiState.Error("Ошибка загрузки: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = CatalogueUiState.Error("Ошибка: ${e.message}")
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun loadNextPage() {
        loadCars(resetPage = false)
    }

    fun applyFilters(newFilters: CatalogueFilters) {
        _filters.value = newFilters
        savedStateHandle[KEY_FILTERS] = newFilters // Сохраняем в SavedStateHandle
        loadCars()
    }

    fun applyCarFilters(carFilters: CarFilters) {
        val catalogueFilters = CatalogueFilters(
            brands = carFilters.brands,
            model = carFilters.model,
            bodyType = carFilters.bodyType,
            carClasses = carFilters.carClasses,
            minYear = carFilters.minYear,
            maxYear = carFilters.maxYear,
            minPrice = carFilters.minCell?.toDouble(),
            maxPrice = carFilters.maxCell?.toDouble(),
            dateStart = carFilters.dateStart?.let { formatDate(it) },
            dateEnd = carFilters.dateEnd?.let { formatDate(it) },
            searchQuery = _filters.value.searchQuery
        )
        applyFilters(catalogueFilters)
    }

    fun getCurrentCarFilters(): CarFilters {
        val f = _filters.value
        return CarFilters(
            brands = f.brands,
            model = f.model,
            bodyType = f.bodyType,
            carClasses = f.carClasses,
            minYear = f.minYear,
            maxYear = f.maxYear,
            minCell = f.minPrice?.toInt(),
            maxCell = f.maxPrice?.toInt(),
            dateStart = f.dateStart?.let { parseDate(it) },
            dateEnd = f.dateEnd?.let { parseDate(it) }
        )
    }

    private fun formatDate(date: Date): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(date)
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun updateSearchQuery(query: String) {
        val newFilters = _filters.value.copy(searchQuery = query)
        _filters.value = newFilters
        savedStateHandle[KEY_FILTERS] = newFilters
        // Поиск можно реализовать локально или через API
        filterCarsBySearch(query)
    }

    private fun filterCarsBySearch(query: String) {
        val currentState = _uiState.value
        if (currentState is CatalogueUiState.Success) {
            if (query.isEmpty()) {
                loadCars()
            } else {
                val filtered = currentState.cars.filter {
                    it.brand.contains(query, ignoreCase = true) ||
                    it.model.contains(query, ignoreCase = true)
                }
                _uiState.value = CatalogueUiState.Success(filtered)
            }
        }
    }

    fun resetFilters() {
        val defaultFilters = CatalogueFilters()
        _filters.value = defaultFilters
        savedStateHandle[KEY_FILTERS] = defaultFilters
        loadCars()
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                // Загрузка брендов
                val brandsResponse = repository.getFilterBrands()
                if (brandsResponse.isSuccessful) {
                    _brands.value = brandsResponse.body() ?: emptyList()
                }

                // Загрузка моделей
                val modelsResponse = repository.getFilterModels()
                if (modelsResponse.isSuccessful) {
                    _models.value = modelsResponse.body() ?: emptyList()
                }

                // Загрузка типов кузова
                val bodyTypesResponse = repository.getFilterBodyTypes()
                if (bodyTypesResponse.isSuccessful) {
                    _bodyTypes.value = bodyTypesResponse.body() ?: emptyList()
                }

                // Загрузка классов
                val classesResponse = repository.getFilterClasses()
                if (classesResponse.isSuccessful) {
                    _carClasses.value = classesResponse.body() ?: emptyList()
                }

                // Загрузка минимальной и максимальной цены
                val priceResponse = repository.getMinMaxCell()
                if (priceResponse.isSuccessful) {
                    priceResponse.body()?.let {
                        _minPrice.value = it.min
                        _maxPrice.value = it.max
                    }
                }
            } catch (e: Exception) {
                // Логирование ошибки
            }
        }
    }

    fun getActiveFiltersCount(): Int {
        val f = _filters.value
        var count = 0
        if (f.brands.isNotEmpty()) count++
        if (f.model != null) count++
        if (f.bodyType != null) count++
        if (f.carClasses.isNotEmpty()) count++
        if (f.minYear != null || f.maxYear != null) count++
        if (f.minPrice != null || f.maxPrice != null) count++
        if (f.dateStart != null || f.dateEnd != null) count++
        return count
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun toggleFavorite(carId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                if (isFavorite) {
                    repository.removeFavorite(carId)
                } else {
                    repository.addFavorite(carId)
                }
                // Refresh current page to update favorite status
                loadCars(resetPage = true)
            } catch (e: Exception) {
                // Handle error silently or show toast
            }
        }
    }

    companion object {
        private const val KEY_FILTERS = "catalogue_filters"
    }
}
