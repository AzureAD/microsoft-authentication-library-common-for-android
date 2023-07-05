package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult

sealed interface SignInTokenApiResult {
    data class Success(val tokenResponse: MicrosoftStsTokenResponse) : SignInTokenApiResult

    data class MFARequired(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInTokenApiResult

    data class UserNotFound(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInTokenApiResult

    data class InvalidCredentials(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInTokenApiResult

    data class CodeIncorrect(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInTokenApiResult

    data class InvalidAuthenticationType(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInTokenApiResult

    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val details: List<Map<String, String>>?
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInTokenApiResult
}