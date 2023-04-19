package com.microsoft.identity.common.java.providers.nativeauth.responses.signin.exceptions

interface ErrorCodes {
    companion object {
        const val VERIFICATION_REQUIRED = "verification_required"
        const val VALIDATION_FAILED = "validation_failed"
        const val CREDENTIAL_REQUIRED = "credential_required"
        const val INVALID_GRANT = "invalid_grant"
    }
}
