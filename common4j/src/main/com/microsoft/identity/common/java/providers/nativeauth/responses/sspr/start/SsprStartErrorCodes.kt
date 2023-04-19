package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start

interface SsprStartErrorCodes {
    companion object {
        const val INVALID_REQUEST = "invalid_request"
        const val INVALID_CLIENT = "invalid_client"
        const val USER_NOT_FOUND = "user_not_found"
        const val UNSUPPORTED_CHALLENGE_TYPE = "unsupported_challenge_type"
    }
}
