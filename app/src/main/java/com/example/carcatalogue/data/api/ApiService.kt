package com.example.carcatalogue.data.api

import com.example.carcatalogue.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    //  AUTHENTICATION 
    
    @POST("api/auth")
    suspend fun login(@Body request: AuthRequest): Response<Unit>
    
    @POST("api/registration")
    suspend fun register(@Body request: RegistrationRequest): Response<UserResponse>
    
    @POST("api/logout")
    suspend fun logout(): Response<Unit>
    
    @POST("api/refresh")
    suspend fun refreshToken(): Response<Unit>
    
    @POST("api/reset-password")
    suspend fun requestPasswordReset(@Body request: ResetPasswordRequest): Response<Unit>
    
    @POST("api/reset")
    suspend fun resetPassword(
        @Query("code") code: String,
        @Body request: RegistrationRequest
    ): Response<Unit>
    
    //  USER PROFILE 
    
    @GET("api/profile/me")
    suspend fun getMe(): Response<Unit>
    
    @GET("api/profile")
    suspend fun getProfile(): Response<UserResponse>
    
    @PATCH("api/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Unit>
    
    @DELETE("api/profile")
    suspend fun deleteProfile(): Response<Unit>
    
    @PATCH("api/profile/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
    
    @GET("api/profile/verify")
    suspend fun verifyEmail(): Response<Unit>
    
    @GET("api/verify")
    suspend fun verifyEmailToken(@Query("code") code: String): Response<Unit>
    
    //  DOCUMENTS 
    
    @GET("api/profile/document")
    suspend fun getDocument(): Response<DocumentResponse>
    
    @POST("api/profile/document")
    suspend fun createDocument(@Body request: CreateDocumentRequest): Response<DocumentResponse>
    
    @PATCH("api/profile/document")
    suspend fun updateDocument(@Body request: UpdateDocumentRequest): Response<DocumentResponse>
    
    @DELETE("api/profile/document")
    suspend fun deleteDocument(): Response<Unit>
    
    //  CAR CATALOGUE 
    
    @GET("api/car/catalogue")
    suspend fun getCatalogue(
        @Query("brand") brand: String? = null,
        @Query("model") model: String? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
        @Query("body_type") bodyType: String? = null,
        @Query("car_class") carClass: String? = null,
        @Query("date_start") dateStart: String? = null,
        @Query("date_end") dateEnd: String? = null,
        @Query("min_cell") minCell: Double? = null,
        @Query("max_cell") maxCell: Double? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<CarListItemResponse>>
    
    @GET("api/car/{carId}")
    suspend fun getCarDetails(@Path("carId") carId: Long): Response<CarDetailResponse>
    
    @GET("api/car/filters/brands")
    suspend fun getFilterBrands(): Response<List<String>>
    
    @GET("api/car/filters/models")
    suspend fun getFilterModels(): Response<List<String>>
    
    @GET("api/car/filters/body-types")
    suspend fun getFilterBodyTypes(): Response<List<String>>
    
    @GET("api/car/filters/classes")
    suspend fun getFilterClasses(): Response<List<String>>
    
    @GET("api/car/filters/min-max-cell")
    suspend fun getMinMaxCell(
        @Query("brand") brand: String? = null,
        @Query("model") model: String? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
        @Query("body_type") bodyType: String? = null,
        @Query("car_class") carClass: String? = null,
        @Query("date_start") dateStart: String? = null,
        @Query("date_end") dateEnd: String? = null
    ): Response<MinMaxCellForFilters>
    
    //  FAVORITES 
    
    @GET("api/cars/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<CarListItemResponse>>
    
    @GET("api/cars/favorites/{carId}")
    suspend fun getFavorite(@Path("carId") carId: Long): Response<CarListItemResponse>
    
    @POST("api/cars/favorites/{carId}")
    suspend fun addFavorite(@Path("carId") carId: Long): Response<CarListItemResponse>
    
    @DELETE("api/cars/favorites/{carId}")
    suspend fun deleteFavorite(@Path("carId") carId: Long): Response<Unit>
    
    //  CONTRACTS 
    
    @GET("api/contracts")
    suspend fun getAllContracts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<ContractResponse>>
    
    @GET("api/contracts/{contractId}")
    suspend fun getContract(@Path("contractId") contractId: Long): Response<ContractResponse>
    
    @POST("api/contracts")
    suspend fun createContract(@Body request: CreateContractRequest): Response<ContractResponse>
    
    @PATCH("api/contracts/{contractId}")
    suspend fun updateContract(
        @Path("contractId") contractId: Long,
        @Body request: UpdateContractRequest
    ): Response<ContractResponse>
    
    @DELETE("api/contracts/{contractId}/cancel")
    suspend fun cancelContract(@Path("contractId") contractId: Long): Response<Unit>
    
    //  ADMIN - CARS 
    
    @GET("api/admin/cars")
    suspend fun getAdminCars(
        @Query("brand") brand: String? = null,
        @Query("model") model: String? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
        @Query("body_type") bodyType: String? = null,
        @Query("car_class") carClass: String? = null,
        @Query("car_state") carState: String? = null,
        @Query("date_start") dateStart: String? = null,
        @Query("date_end") dateEnd: String? = null,
        @Query("min_cell") minCell: Double? = null,
        @Query("max_cell") maxCell: Double? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<CarListItemResponse>>
    
    @GET("api/admin/cars/{carId}")
    suspend fun getAdminCar(@Path("carId") carId: Long): Response<CarDetailResponse>
    
    @Multipart
    @POST("api/admin/cars")
    suspend fun createCar(
        @Part("car") car: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<CarDetailResponse>
    
    @PATCH("api/admin/cars/{carId}")
    suspend fun updateCar(
        @Path("carId") carId: Long,
        @Body request: UpdateCarRequest
    ): Response<CarDetailResponse>
    
    @DELETE("api/admin/cars/{carId}")
    suspend fun deleteCar(@Path("carId") carId: Long): Response<Unit>
    
    @PATCH("api/admin/cars/{carId}/state")
    suspend fun updateCarState(
        @Path("carId") carId: Long,
        @Body request: UpdateCarStateRequest
    ): Response<CarDetailResponse>
    
    @GET("api/admin/cars/state")
    suspend fun getAllCarStates(): Response<List<CarStateResponse>>
    
    @Multipart
    @POST("api/admin/{id}/images")
    suspend fun uploadCarImage(
        @Path("id") carId: Long,
        @Part file: MultipartBody.Part
    ): Response<ImageResponse>
    
    //  ADMIN - MODELS 
    
    @GET("api/admin/models")
    suspend fun getModels(
        @Query("brand") brand: String? = null,
        @Query("body_type") bodyType: String? = null,
        @Query("car_class") carClass: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<CarModelResponse>>
    
    @GET("api/admin/models/{modelId}")
    suspend fun getModelById(@Path("modelId") modelId: Long): Response<CarModelResponse>
    
    @POST("api/admin/models")
    suspend fun createModel(@Body request: CreateCarModelRequest): Response<CarModelResponse>
    
    @PUT("api/admin/models/{modelId}")
    suspend fun updateModel(
        @Path("modelId") modelId: Long,
        @Body request: UpdateCarModelRequest
    ): Response<CarModelResponse>
    
    @DELETE("api/admin/models/{modelId}")
    suspend fun deleteModel(@Path("modelId") modelId: Long): Response<Unit>
    
    @GET("api/admin/filters/brands")
    suspend fun getAdminBrands(): Response<List<String>>
    
    @POST("api/admin/filters/brands")
    suspend fun createBrand(@Body request: CreateCarModelsBrand): Response<BrandResponse>
    
    @GET("api/admin/filters/models")
    suspend fun getAdminModels(): Response<List<String>>
    
    @POST("api/admin/filters/models")
    suspend fun createModelName(@Body request: CreateCarModelName): Response<ModelNameResponse>
    
    @GET("api/admin/filters/classes")
    suspend fun getAdminClasses(): Response<List<String>>
    
    @POST("api/admin/filters/classes")
    suspend fun createClass(@Body request: CreateCarModelName): Response<ModelNameResponse>
    
    //  ADMIN - CONTRACTS 
    
    @GET("api/admin/contracts")
    suspend fun getAdminContracts(
        @Query("status") status: String? = null,
        @Query("idUser") idUser: Long? = null,
        @Query("idCar") idCar: Long? = null,
        @Query("brand") brand: String? = null,
        @Query("body_type") bodyType: String? = null,
        @Query("car_class") carClass: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<ContractResponse>>
    
    @GET("api/admin/contracts/{contractId}")
    suspend fun getAdminContract(@Path("contractId") contractId: Long): Response<ContractResponse>
    
    @PATCH("api/admin/contracts/{contractId}/confirm")
    suspend fun confirmContract(@Path("contractId") contractId: Long): Response<ContractResponse>
    
    @DELETE("api/admin/contracts/{contractId}/cancel")
    suspend fun cancelAdminContract(@Path("contractId") contractId: Long): Response<Unit>
    
    @PATCH("api/admin/contracts/{id}/confirm-cancellation")
    suspend fun confirmCancellation(@Path("id") contractId: Long): Response<Unit>
    
    //  ADMIN - CLIENTS 
    
    @GET("api/admin/users")
    suspend fun getAllUsers(
        @Query("banned") banned: Boolean? = null,
        @Query("roleName") roleName: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<AllUserResponse>>
    
    @GET("api/admin/users/{userId}")
    suspend fun getUser(@Path("userId") userId: Long): Response<AllUserResponse>
    
    @PATCH("api/admin/users/{userId}/updateRole")
    suspend fun updateUserRole(
        @Path("userId") userId: Long,
        @Body request: RoleRequest
    ): Response<Unit>
    
    @PATCH("api/admin/users/{userId}/ban")
    suspend fun banUser(@Path("userId") userId: Long): Response<Unit>
    
    @PATCH("api/admin/users/{userId}/unban")
    suspend fun unbanUser(@Path("userId") userId: Long): Response<Unit>
    
    //  ADMIN - DOCUMENTS 
    
    @GET("api/admin/documents")
    suspend fun getAllDocuments(
        @Query("onlyUnverified") onlyUnverified: Boolean = true,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String? = null
    ): Response<PagedModel<DocumentResponse>>
    
    @PATCH("api/admin/documents/{documentId}/verify")
    suspend fun verifyDocument(@Path("documentId") documentId: Long): Response<Unit>
}