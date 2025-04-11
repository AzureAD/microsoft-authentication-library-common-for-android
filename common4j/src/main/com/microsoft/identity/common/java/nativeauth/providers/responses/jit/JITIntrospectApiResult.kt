package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.AuthenticationMethodApiResult

/**
 * Represents the potential result types returned from the register/introspect endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface JITIntrospectApiResult: ApiResult {
    data class Redirect(
        override val correlationId: String
    ) : JITIntrospectApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class Success(
        override val correlationId: String,
        val continuationToken: String,
        val methods: List<AuthenticationMethodApiResult>
    ) : JITIntrospectApiResult {
        override fun toUnsanitizedString(): String {
            return "Success(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class UnknownError(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), JITIntrospectApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}