package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.IApiResponse
import com.microsoft.identity.common.java.nativeauth.util.isInvalidChallengeTarget
import com.microsoft.identity.common.java.nativeauth.util.isInvalidRequest
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import java.net.HttpURLConnection

/**
 * Represents the raw response from the register/challenge endpoint.
 * Can be converted to JITChallengeApiResult using the provided toResult() method.
 */
class JITChallengeApiResponse(
    @Expose override var statusCode: Int,
    correlationId: String,
    @SerializedName("continuation_token") val continuationToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @Expose @SerializedName("challenge_target") val challengeTarget: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @Expose @SerializedName("code_length") val codeLength: Int?,
    @Expose @SerializedName("interval") val interval: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("error_codes") val errorCodes: List<Int>?,
    @SerializedName("error_description") val errorDescription: String?,
    @SerializedName("error_uri") val errorUri: String?,
) : IApiResponse(statusCode, correlationId) {
    override fun toUnsanitizedString(): String {
        return "JITChallengeApiResponse(statusCode=$statusCode, " +
                "correlationId=$correlationId " +
                "error=$error, errorCodes=$errorCodes, errorDescription=$errorDescription)"
    }

    override fun toString(): String = "JITChallengeApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId"

    fun toResult(): JITChallengeApiResult {
        return when (statusCode) {
            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                return when {
                    error.isInvalidRequest() && errorCodes?.first().isInvalidChallengeTarget() -> {
                        JITChallengeApiResult.InvalidVerificationContact(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        JITChallengeApiResult.UnknownError(
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
                    challengeType.isRedirect() -> {
                        JITChallengeApiResult.Redirect(
                            correlationId = correlationId
                        )
                    }
                    continuationToken.isNullOrBlank() ||
                    challengeType.isNullOrBlank() ||
                            challengeTarget.isNullOrBlank() ||
                            challengeChannel.isNullOrBlank() ||
                            codeLength == null -> {
                        JITChallengeApiResult.UnknownError(
                            error = "invalid_state",
                            errorDescription = "Register authentication method /challenge did not return all mandatory fields",
                            errorCodes = errorCodes.orEmpty(),
                            correlationId = correlationId
                        )
                    }
                    else -> {
                        JITChallengeApiResult.Success(
                            correlationId = correlationId,
                            continuationToken = continuationToken,
                            challengeType = challengeType,
                            bindingMethod = bindingMethod,
                            challengeTargetLabel = challengeTarget,
                            challengeChannel = challengeChannel,
                            codeLength = codeLength
                        )
                    }
                }
            }
            else -> {
                JITChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    errorCodes = errorCodes.orEmpty(),
                    correlationId = correlationId
                )
            }
        }
    }
}