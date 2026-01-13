package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class UserStats(
    @SerializedName("favoriteCarId") val favoriteCarId: Long?,
    @SerializedName("favoriteCarBrand") val favoriteCarBrand: String?,
    @SerializedName("favoriteCarModelName") val favoriteCarModelName: String?,
    @SerializedName("favoriteCarCarClass") val favoriteCarCarClass: String?,
    @SerializedName("totalRides") val totalRides: Int?,
    @SerializedName("ridesThisMonth") val ridesThisMonth: Long?,
    @SerializedName("totalSpent") val totalSpent: Long?,
    @SerializedName("favoriteBrand") val favoriteBrand: String?,
    @SerializedName("topUsedCarClass") val topUsedCarClass: String?,
    @SerializedName("lastRideDate") val lastRideDate: String?,
    @SerializedName("averageTimeDrive") val averageTimeDrive: Double?,
    @SerializedName("averageTimeToStartDrive") val averageTimeToStartDrive: String?,
    @SerializedName("averageCost") val averageCost: Double?
)
