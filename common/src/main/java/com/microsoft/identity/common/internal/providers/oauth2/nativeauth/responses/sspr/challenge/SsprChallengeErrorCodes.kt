package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.challenge

interface SsprChallengeErrorCodes {
    companion object {
        const val INVALID_REQUEST = "invalid_request"
        const val INVALID_CLIENT = "invalid_client"
        const val EXPIRED_TOKEN = "expired_token"
        const val UNSUPPORTED_CHALLENGE_TYPE = "unsupported_challenge_type"
    }
}
