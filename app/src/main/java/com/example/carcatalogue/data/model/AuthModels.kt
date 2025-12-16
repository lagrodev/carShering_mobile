package com.example.carcatalogue.data.model

// Authentication
data class JwtRequest(
    val username: String,
    val password: String
)

data class JwtResponse(
    val token: String
)


