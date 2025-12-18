package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class ContractResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("totalCost") val totalCost: Double,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("bodyType") val bodyType: String,
    @SerializedName("carClass") val carClass: String,
    @SerializedName("yearOfIssue") val yearOfIssue: Int,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("vin") val vin: String,
    @SerializedName("gosNumber") val gosNumber: String,
    @SerializedName("state") val state: ContractState?
)

enum class ContractState {
    @SerializedName("PENDING") PENDING,
    @SerializedName("CONFIRMED") CONFIRMED,
    @SerializedName("ACTIVE") ACTIVE,
    @SerializedName("COMPLETED") COMPLETED,
    @SerializedName("CANCELLED") CANCELLED,
    @SerializedName("CANCELLATION_REQUESTED") CANCELLATION_REQUESTED,
    // backward-compat if backend ever used this name
    @SerializedName("AWAITING_CANCELLATION") AWAITING_CANCELLATION
}

data class CreateContractRequest(
    @SerializedName("carId") val carId: Long,
    @SerializedName("dataStart") val dataStart: String,
    @SerializedName("dataEnd") val dataEnd: String,
    @SerializedName("dailyRate") val dailyRate: Double
)

data class UpdateContractRequest(
    @SerializedName("dataStart") val dataStart: String,
    @SerializedName("dataEnd") val dataEnd: String,
    @SerializedName("dailyRate") val dailyRate: Double?
)
