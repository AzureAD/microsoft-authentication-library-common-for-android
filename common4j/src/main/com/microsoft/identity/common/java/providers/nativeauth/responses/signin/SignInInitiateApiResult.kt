package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

sealed interface SignInInitiateApiResult {
    object Redirect : SignInInitiateApiResult
    data class UserNotFound(val error: String, val errorDescription: String) : SignInInitiateApiResult
    data class Success(val credentialToken: String) : SignInInitiateApiResult
    data class UnknownError(val error: String?, val errorDescription: String?) :
        SignInInitiateApiResult
}