package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

sealed interface SignInChallengeApiResult {
    object Redirect : SignInChallengeApiResult
    data class OOBRequired(
        val credentialToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int) : SignInChallengeApiResult
    data class PasswordRequired(val credentialToken: String) : SignInChallengeApiResult
    data class UnknownError(val error: String?, val errorDescription: String?) :
        SignInChallengeApiResult
}