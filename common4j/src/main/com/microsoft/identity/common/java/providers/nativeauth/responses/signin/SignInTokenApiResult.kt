package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse

sealed interface SignInTokenApiResult {
    data class Success(val tokenResponse: MicrosoftStsTokenResponse) : SignInTokenApiResult
    data class MFARequired(val error: String, val errorDescription: String, val errorCodes: List<Int>) : SignInTokenApiResult
    data class UserNotFound(val error: String, val errorDescription: String, val errorCodes: List<Int>) : SignInTokenApiResult
    data class InvalidCredentials(val error: String, val errorDescription: String, val errorCodes: List<Int>) : SignInTokenApiResult
    data class CodeIncorrect(val error: String, val errorDescription: String, val errorCodes: List<Int>) : SignInTokenApiResult
    data class InvalidAuthenticationType(val error: String, val errorDescription: String, val errorCodes: List<Int>) : SignInTokenApiResult
    data class UnknownError(val error: String?, val errorDescription: String?, val details: List<Map<String, String>>?, val errorCodes: List<Int>?) :
        SignInTokenApiResult
}