package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult

/**
 * Represents the potential result types returned from the register/challenge endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface JITChallengeApiResult: ApiResult {
    data class Redirect(
        override val correlationId: String
    ) : JITChallengeApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class Success(
        override val correlationId: String,
        val continuationToken: String,
        val challengeType: String,
        val bindingMethod: String?,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : JITChallengeApiResult {
        override fun toUnsanitizedString(): String {
            return "Success(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class InvalidVerificationContact(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), JITChallengeApiResult {
        override fun toUnsanitizedString() = "InvalidVerificationContact(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "InvalidVerificationContact(correlationId=$correlationId)"
    }

    data class UnknownError(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), JITChallengeApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}