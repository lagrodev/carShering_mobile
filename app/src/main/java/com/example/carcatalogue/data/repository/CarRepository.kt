package com.example.carcatalogue.data.repository

import com.example.carcatalogue.data.api.ApiService
import com.example.carcatalogue.data.model.*
import retrofit2.Response

class CarRepository(private val apiService: ApiService) {
    
    suspend fun getCatalogue(
        brands: List<String>? = null,
        model: String? = null,
        minYear: Int? = null,
        maxYear: Int? = null,
        bodyType: String? = null,
        carClasses: List<String>? = null,
        dateStart: String? = null,
        dateEnd: String? = null,
        minCell: Double? = null,
        maxCell: Double? = null,
        page: Int = 0,
        size: Int = 20,
        sort: String? = null
    ): Response<PagedModel<CarListItemResponse>> {
        return apiService.getCatalogue(
            brands = brands,
            model = model,
            minYear = minYear,
            maxYear = maxYear,
            bodyType = bodyType,
            carClasses = carClasses,
            dateStart = dateStart,
            dateEnd = dateEnd,
            minCell = minCell,
            maxCell = maxCell,
            page = page,
            size = size,
            sort = sort
        )
    }
    
    suspend fun getCarDetail(carId: Long): Response<CarDetailResponse> {
        return apiService.getCarDetails(carId)
    }
    
    suspend fun getFilterBrands(): Response<List<String>> {
        return apiService.getFilterBrands()
    }
    
    suspend fun getFilterModels(): Response<List<String>> {
        return apiService.getFilterModels()
    }
    
    suspend fun getFilterClasses(): Response<List<String>> {
        return apiService.getFilterClasses()
    }
    
    suspend fun getFilterBodyTypes(): Response<List<String>> {
        return apiService.getFilterBodyTypes()
    }

    suspend fun getMinMaxCell(
        brand: String? = null,
        model: String? = null,
        minYear: Int? = null,
        maxYear: Int? = null,
        bodyType: String? = null,
        carClass: String? = null,
        dateStart: String? = null,
        dateEnd: String? = null
    ): Response<MinMaxCellForFilters> {
        return apiService.getMinMaxCell(
            brand = brand,
            model = model,
            minYear = minYear,
            maxYear = maxYear,
            bodyType = bodyType,
            carClass = carClass,
            dateStart = dateStart,
            dateEnd = dateEnd
        )
    }
    
    // Favorites methods
    suspend fun getFavorites(
        page: Int = 0,
        size: Int = 20,
        sort: String? = null
    ): Response<PagedModel<CarListItemResponse>> {
        return apiService.getFavorites(page = page, size = size, sort = sort)
    }
    
    suspend fun addFavorite(carId: Long): Response<CarListItemResponse> {
        return apiService.addFavorite(carId)
    }
    
    suspend fun removeFavorite(carId: Long): Response<Unit> {
        return apiService.deleteFavorite(carId)
    }
    
    suspend fun checkFavorite(carId: Long): Response<CarListItemResponse> {
        return apiService.getFavorite(carId)
    }
}
