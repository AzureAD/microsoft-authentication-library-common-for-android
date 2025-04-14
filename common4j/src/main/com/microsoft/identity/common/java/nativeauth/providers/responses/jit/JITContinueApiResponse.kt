package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.util.isInvalidGrant
import com.microsoft.identity.common.java.nativeauth.util.isOOBValueInvalid
import java.net.HttpURLConnection

/**
 * Represents the raw response from the register/continue endpoint.
 * Can be converted to JITContinueAPIResult using the provided toResult() method.
 */
class JITContinueApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("suberror") val subError: String?
) : IApiResponse(statusCode, correlationId) {
    override fun toUnsanitizedString(): String {
        return "JITContinueAPIResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId " +
                "error=$error, errorCodes=$errorCodes, errorDescription=$errorDescription)"
    }

    override fun toString(): String = "JITContinueAPIResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId"

    fun toResult(): JITContinueApiResult {
        return when (statusCode) {
            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isInvalidGrant() && subError.isOOBValueInvalid() -> {
                        JITContinueApiResult.CodeIncorrect(
                            correlationId = correlationId,
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            errorCodes = errorCodes.orEmpty(),
                            subError = subError.orEmpty()
                        )
                    }
                    else -> {
                        JITContinueApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    continuationToken.isNullOrBlank() -> {
                        JITContinueApiResult.UnknownError(
                            error = "invalid_state",
                            errorDescription = "Register authentication method /continue did not return continuationToken field",
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        JITContinueApiResult.Success(
                            correlationId = correlationId,
                            continuationToken = continuationToken
                        )
                    }
                }
            }
            else -> {
                JITContinueApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}