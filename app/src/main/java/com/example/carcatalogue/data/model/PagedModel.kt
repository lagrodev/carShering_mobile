package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class PagedModel<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("page") val page: PageMetadata
)

data class PageMetadata(
    @SerializedName("size") val size: Long,
    @SerializedName("number") val number: Long,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Long
)
