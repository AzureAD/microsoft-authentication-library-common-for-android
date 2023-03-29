package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.exceptions

interface ErrorCodes {
    companion object {
        const val VERIFICATION_REQUIRED = "verification_required"
        const val VALIDATION_FAILED = "validation_failed"
    }
}
