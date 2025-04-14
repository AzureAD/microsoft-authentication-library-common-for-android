package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult

/**
 * Represents the potential result types returned from the register/continue endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface JITContinueApiResult: ApiResult {
    data class Redirect(
        override val correlationId: String
    ) : JITContinueApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class Success(
        override val correlationId: String,
        val continuationToken: String
    ) : JITContinueApiResult {
        override fun toUnsanitizedString(): String {
            return "Success(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class CodeIncorrect(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val subError: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId,
        errorCodes = errorCodes
    ), JITContinueApiResult {
        override fun toUnsanitizedString() = "CodeIncorrect(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "CodeIncorrect(correlationId=$correlationId)"
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
    ), JITContinueApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}