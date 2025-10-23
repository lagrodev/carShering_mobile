package com.example.carcatalogue.data.model


data class CarListItemResponse(
    val id: Long,
    val brand: String,
    val carClass: String,
    val model: String,
    val yearOfIssue: Int,
    val rent: Double
)