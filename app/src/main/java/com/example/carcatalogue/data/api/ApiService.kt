package com.example.carcatalogue.data.api
import com.example.carcatalogue.data.model.CarDetailResponse
import com.example.carcatalogue.data.model.CarListItemResponse
import com.example.carcatalogue.data.model.CarModelResponse
import com.example.carcatalogue.data.model.PageResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/car/catalogue")
    suspend fun getCatalogue(
        @Query("brand") brand: String? = null,
        @Query("model") model: String? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
        @Query("body_type") bodyType: String? = null,
        @Query("car_class") carClass: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "model.brand"
    ): Response<PageResponse<CarListItemResponse>>

    @GET("api/car/{carId}")
    suspend fun getCarDetail(@Path("carId") carId: Long): Response<CarDetailResponse>

    @GET("api/car/models")
    suspend fun getBrands(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100 // чтобы получить все бренды за раз
    ):Response<PageResponse<CarModelResponse>>

}