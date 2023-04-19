package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont

interface SsprContinueErrorCodes {
    companion object {
        const val INVALID_REQUEST = "invalid_request"
        const val INVALID_CLIENT = "invalid_client"
        const val INVALID_GRANT = "invalid_grant"
        const val EXPIRED_TOKEN = "expired_token"
    }
}
