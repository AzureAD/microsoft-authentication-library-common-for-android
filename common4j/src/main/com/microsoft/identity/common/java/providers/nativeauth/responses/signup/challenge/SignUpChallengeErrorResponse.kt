package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeErrorCodes

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
