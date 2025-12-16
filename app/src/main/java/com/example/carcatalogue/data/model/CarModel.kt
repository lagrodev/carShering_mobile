package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class CarModelResponse(
    @SerializedName("modelId") val modelId: Long,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("bodyType") val bodyType: String,
    @SerializedName("carClass") val carClass: String,
    @SerializedName("isDeleted") val isDeleted: Boolean
)

data class CreateCarModelRequest(
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("bodyType") val bodyType: String,
    @SerializedName("carClass") val carClass: String
)

data class UpdateCarModelRequest(
    @SerializedName("brand") val brand: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("bodyType") val bodyType: String?,
    @SerializedName("carClass") val carClass: String?
)

data class BrandResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class ModelNameResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class CreateCarModelName(
    @SerializedName("name") val name: String
)

data class CreateCarModelsBrand(
    @SerializedName("name") val name: String
)
