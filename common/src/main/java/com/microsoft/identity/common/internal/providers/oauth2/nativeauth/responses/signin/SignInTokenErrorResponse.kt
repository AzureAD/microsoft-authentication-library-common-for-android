package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.logging.Logger

data class SignInTokenErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: String?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
    @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
    @SerializedName("credential_token ") val credentialToken: String?
) : IApiErrorResponse {
    private val TAG = SignInTokenErrorResponse::class.java.simpleName

    override fun getError() = errorCode

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (error.isNullOrEmpty()) {
            throw ClientException("SignInTokenErrorResponse error can't be null in error state")
        }
    }

    override fun validateOptionalFields() {
        if (getErrorDescription().isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInTokenErrorResponse errorDescription is null or empty")
        }
        if (errorUri.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInTokenErrorResponse errorUri is null or empty")
        }
        if (innerErrors.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInTokenErrorResponse innerErrors is null or empty")
        }
        if (credentialToken.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInTokenErrorResponse credentialToken is null or empty")
        }
    }
}
