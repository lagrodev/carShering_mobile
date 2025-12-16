package com.example.carcatalogue.data.model

import com.google.gson.annotations.SerializedName

data class DocumentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("documentType") val documentType: String,
    @SerializedName("series") val series: String,
    @SerializedName("number") val number: String,
    @SerializedName("dateOfIssue") val dateOfIssue: String,
    @SerializedName("issuingAuthority") val issuingAuthority: String,
    @SerializedName("verified") val verified: Boolean
)

data class CreateDocumentRequest(
    @SerializedName("documentTypeId") val documentTypeId: Long,
    @SerializedName("series") val series: String,
    @SerializedName("number") val number: String,
    @SerializedName("dateOfIssue") val dateOfIssue: String,
    @SerializedName("issuingAuthority") val issuingAuthority: String
)

data class UpdateDocumentRequest(
    @SerializedName("documentTypeId") val documentTypeId: Long?,
    @SerializedName("series") val series: String?,
    @SerializedName("number") val number: String?,
    @SerializedName("dateOfIssue") val dateOfIssue: String?,
    @SerializedName("issuingAuthority") val issuingAuthority: String?
)
