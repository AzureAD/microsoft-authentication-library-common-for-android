package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup

import com.google.gson.annotations.SerializedName

enum class SignUpStartErrorCodes {
    @SerializedName("unsupported_challenge_type")
    UNSUPPORTED_CHALLENGE_TYPE,

    @SerializedName("auth_not_supported")
    AUTH_NOT_SUPPORTED,

    @SerializedName("verification_required")
    VERIFICATION_REQUIRED,

    @SerializedName("validation_failed")
    VALIDATION_FAILED
}

enum class SignUpChallengeErrorCodes {
    @SerializedName("unsupported_challenge_type")
    UNSUPPORTED_CHALLENGE_TYPE,

    @SerializedName("auth_not_supported")
    AUTH_NOT_SUPPORTED,

    @SerializedName("expired_token")
    EXPIRED_TOKEN
}

enum class SignUpContinueErrorCodes {
    @SerializedName("invalid_grant")
    INVALID_GRANT,

    @SerializedName("expired_token")
    EXPIRED_TOKEN,

    @SerializedName("validation_failed")
    VALIDATION_FAILED,

    @SerializedName("verification_required")
    VERIFICATION_REQUIRED,

    @SerializedName("attributes_required")
    ATTRIBUTES_REQUIRED
}
