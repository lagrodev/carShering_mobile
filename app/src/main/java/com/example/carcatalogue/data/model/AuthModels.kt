package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

// Authentication
data class JwtRequest(
    val username: String,
    val password: String
)

data class JwtResponse(
    @SerializedName("token") val token: String?
)


