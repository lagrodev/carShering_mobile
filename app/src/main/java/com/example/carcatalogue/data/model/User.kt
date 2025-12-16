package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("login") val login: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String,
    @SerializedName("emailVerified") val emailVerified: Boolean
)

data class AllUserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("login") val login: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String,
    @SerializedName("roleName") val roleName: String,
    @SerializedName("banned") val banned: Boolean,
    @SerializedName("emailVerified") val emailVerified: Boolean
)

data class AuthRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RegistrationRequest(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("RoleId") val roleId: Long = 2 // Default USER role
)

data class UpdateProfileRequest(
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("phone") val phone: String?
)

data class ChangePasswordRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String
)

data class RoleRequest(
    @SerializedName("RoleName") val roleName: String
)
