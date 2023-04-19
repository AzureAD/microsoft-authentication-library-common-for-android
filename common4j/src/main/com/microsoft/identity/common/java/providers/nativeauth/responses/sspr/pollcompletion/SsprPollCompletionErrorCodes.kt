package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion

interface SsprPollCompletionErrorCodes {
    companion object {
        const val INVALID_REQUEST = "invalid_request"
        const val INVALID_CLIENT = "invalid_client"
        const val EXPIRED_TOKEN = "expired_token"
        const val PASSWORD_TOO_WEAK = "password_too_weak"
        const val PASSWORD_TOO_SHORT = "password_too_short"
        const val PASSWORD_TOO_LONG = "password_too_long"
        const val PASSWORD_RECENTLY_USED = "password_recently_used"
        const val PASSWORD_BANNED = "password_banned"
        const val USER_NOT_FOUND = "user_not_found"
    }
}
