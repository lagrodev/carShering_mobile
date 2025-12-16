package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class PageResponse<T>(
    val content: List<T>,
    val page: PageInfo
)

data class PageInfo(
    val size: Int,
    val number: Int,
    val totalElements: Long,
    val totalPages: Int
)