package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult

sealed interface SignInChallengeApiResult {
    object Redirect : SignInChallengeApiResult
    data class OOBRequired(
        val credentialToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : SignInChallengeApiResult

    data class PasswordRequired(val credentialToken: String) : SignInChallengeApiResult

    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>,
        override val details: List<Map<String, String>>?
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        details = details
    ), SignInChallengeApiResult
}