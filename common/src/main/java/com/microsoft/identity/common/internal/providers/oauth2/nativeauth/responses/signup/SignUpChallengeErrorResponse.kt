package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException

data class SignUpChallengeErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: String?,
    @SerializedName("error_description") private val errorDescription: String?
) : com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiErrorResponse {
    override fun getError() = errorCode

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (error == null || error!!.isEmpty()) {
            throw ClientException("SignUpChallengeResponse.error can't be null in error state")
        }
        if (getErrorDescription() == null || getErrorDescription()!!.isEmpty()) {
            throw ClientException("SignUpChallengeResponse.errorDescription can't be null in error state")
        }
    }

    override fun validateOptionalFields() {
        // No optional fields
    }
}
