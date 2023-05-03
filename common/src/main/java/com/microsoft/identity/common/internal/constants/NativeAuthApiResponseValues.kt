package com.microsoft.identity.common.internal.constants

interface NativeAuthApiResponseValues {
    companion object {
        object ErrorCodes {
            const val CREDENTIAL_REQUIRED = "credential_required"
        }

        object ChallengeTypes {
            const val REDIRECT = "redirect"
        }
    }
}
