package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class CarListItemResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("brand") val brand: String,
    @SerializedName("carClass") val carClass: String,
    @SerializedName("model") val model: String,
    @SerializedName("yearOfIssue") val yearOfIssue: Int,
    @SerializedName("rent") val rent: Double,
    @SerializedName("status") val status: String,
    @SerializedName("favorite") val favorite: Boolean = false,
    @SerializedName("imageUrl") val imageUrl: String? = null
)

data class CarDetailResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("modelId") val modelId: Long,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("bodyType") val bodyType: String,
    @SerializedName("carClass") val carClass: String,
    @SerializedName("yearOfIssue") val yearOfIssue: Int,
    @SerializedName("gosNumber") val gosNumber: String,
    @SerializedName("vin") val vin: String,
    @SerializedName("status") val status: String,
    @SerializedName("rent") val rent: Double,
    @SerializedName("favorite") val favorite: Boolean = false,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class CreateCarRequest(
    @SerializedName("modelId") val modelId: Long,
    @SerializedName("yearOfIssue") val yearOfIssue: Int,
    @SerializedName("gosNumber") val gosNumber: String,
    @SerializedName("vin") val vin: String,
    @SerializedName("rent") val rent: Double,
    @SerializedName("stateId") val stateId: Long
)

data class UpdateCarRequest(
    @SerializedName("modelId") val modelId: Long?,
    @SerializedName("yearOfIssue") val yearOfIssue: Int?,
    @SerializedName("gosNumber") val gosNumber: String?,
    @SerializedName("vin") val vin: String?,
    @SerializedName("rent") val rent: Double?
)

data class UpdateCarStateRequest(
    @SerializedName("stateName") val stateName: String
)

data class CarStateResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("status") val status: String
)

data class MinMaxCellForFilters(
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double
)

data class ImageResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("url") val url: String
)
