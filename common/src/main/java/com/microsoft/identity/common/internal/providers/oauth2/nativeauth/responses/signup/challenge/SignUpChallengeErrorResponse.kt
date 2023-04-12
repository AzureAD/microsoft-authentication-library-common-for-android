package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.challenge

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeErrorCodes
import com.microsoft.identity.common.java.exception.ClientException

data class SignUpChallengeErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: SignUpChallengeErrorCodes?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("challenge_type") val challengeType: NativeAuthChallengeType?,
    @SerializedName("signup_token")val signupToken: String?
) : IApiErrorResponse {
    private val TAG = SignUpChallengeErrorResponse::class.java.simpleName

    override fun getError() = errorCode.toString()

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (errorCode == null) {
            throw ClientException("SignUpChallengeErrorResponse.error=$errorCode can't be null or invalid in error state")
        }
    }

    override fun validateOptionalFields() {}
}
