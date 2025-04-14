package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.AuthenticationMethodApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.toListOfAuthenticationMethodApiResult
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import java.lang.IllegalStateException
import java.net.HttpURLConnection

/**
 * Represents the raw response from the register/introspect endpoint.
 * Can be converted to JITIntrospectApiResult using the provided toResult() method.
 */
class JITIntrospectApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("methods") val methods: List<AuthenticationMethodApiResponse>?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
) : IApiResponse(statusCode, correlationId) {

    override fun toUnsanitizedString(): String {
        return "JITIntrospectApiResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId " +
                "error=$error, errorCodes=$errorCodes, errorDescription=$errorDescription)"
    }

    override fun toString(): String = "JITIntrospectApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId"

    fun toResult(): JITIntrospectApiResult {
        return when (statusCode) {
            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                JITIntrospectApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    correlationId = correlationId
                )
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    challengeType.isRedirect() -> {
                        JITIntrospectApiResult.Redirect(
                            correlationId = correlationId
                        )
                    }
                    methods.isNullOrEmpty() -> {
                        JITIntrospectApiResult.UnknownError(
                            error = ApiErrorResult.INVALID_STATE,
                            errorDescription = "register/introspect did not return methods",
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        try {
                            JITIntrospectApiResult.Success(
                                correlationId = correlationId,
                                continuationToken = continuationToken
                                    ?: return JITIntrospectApiResult.UnknownError(
                                        error = ApiErrorResult.INVALID_STATE,
                                        errorDescription = "register/introspect did not return a continuation token",
                                        errorCodes = errorCodes.orEmpty(),
                                        correlationId = correlationId
                                    ),
                                methods = methods.toListOfAuthenticationMethodApiResult()
                            )
                        } catch (e: IllegalStateException) {
                            JITIntrospectApiResult.UnknownError(
                                error = ApiErrorResult.INVALID_STATE,
                                errorDescription = "register/introspect did not return valid methods: ${e.message}",
                                errorCodes = errorCodes.orEmpty(),
                                correlationId = correlationId
                            )
                        }
                    }
                }
            }
            else -> {
                JITIntrospectApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}