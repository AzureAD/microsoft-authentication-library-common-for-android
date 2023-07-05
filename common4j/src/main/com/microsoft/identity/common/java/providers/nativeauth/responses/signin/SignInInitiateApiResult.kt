package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult

sealed interface SignInInitiateApiResult {
    object Redirect : SignInInitiateApiResult

    data class Success(val credentialToken: String) : SignInInitiateApiResult

    data class UserNotFound(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInInitiateApiResult

    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val details: List<Map<String, String>>?
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes
    ), SignInInitiateApiResult
}