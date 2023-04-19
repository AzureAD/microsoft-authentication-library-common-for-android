package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.IApiErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger

data class SignInInitiateErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: String?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("inner_errors") val innerErrors: List<InnerError>?
) : IApiErrorResponse {
    private val TAG = SignInInitiateErrorResponse::class.java.simpleName

    override fun getError() = errorCode

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (error.isNullOrEmpty()) {
            throw ClientException("SignInInitiateErrorResponse error can't be null in error state")
        }
    }

    override fun validateOptionalFields() {
        if (getErrorDescription().isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInInitiateErrorResponse errorDescription is null or empty")
        }
        if (errorUri.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInInitiateErrorResponse errorUri is null or empty")
        }
        if (innerErrors.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInInitiateErrorResponse innerErrors is null or empty")
        }
    }
}
