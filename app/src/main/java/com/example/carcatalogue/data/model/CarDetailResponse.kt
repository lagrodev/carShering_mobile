package com.example.carcatalogue.data.model

data class CarDetailResponse(
    val id: Long,
    val brand: String,
    val model: String,
    val bodyType: String,
    val carClass: String,
    val yearOfIssue: Int,
    val gosNumber: String,
    val vin: String,
    val status: String,
    val rent: Double
)