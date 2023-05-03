package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiErrorResponse

data class SsprSubmitErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val error: String?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("error_url") val errorUrl: String?,
    @SerializedName("target") val target: String?,
    @SerializedName("error_code") val errorCode: String?
) : IApiErrorResponse {
    override fun getError() = error

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (error.isNullOrBlank()) {
            throw ClientException("SsprSubmitErrorResponse.error can't be null in error state")
        }
    }

    override fun validateOptionalFields() {}
}
