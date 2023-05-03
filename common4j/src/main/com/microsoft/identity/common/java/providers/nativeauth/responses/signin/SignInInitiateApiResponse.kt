package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isRedirect
import com.microsoft.identity.common.java.util.isUserAccountDoesNotExist
import java.net.HttpURLConnection

// TODO are these fields considered PII?
data class SignInInitiateApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
): IApiResponse(statusCode) {

    fun toResult(): SignInInitiateApiResult {
        return if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            if (error.isInvalidGrant()) {
                if (errorCodes.isNullOrEmpty()) {
                    throw ClientException("error_codes is null or empty")
                } else if (errorCodes[0].isUserAccountDoesNotExist()) {
                    return SignInInitiateApiResult.UserNotFound(error = error.orEmpty(), errorDescription = errorDescription.orEmpty())
                } else {
                    return SignInInitiateApiResult.UnknownError(error, errorDescription)
                }
            }
            // TODO log the API response, in a PII-safe way
            SignInInitiateApiResult.UnknownError(error, errorDescription)
        } else {
            if (challengeType.isRedirect()) {
                SignInInitiateApiResult.Redirect
            }
            else if (credentialToken.isNullOrBlank()) {
                throw ClientException("credential_token is null or blank")
            } else {
                SignInInitiateApiResult.Success(credentialToken)
            }
        }
    }
}

