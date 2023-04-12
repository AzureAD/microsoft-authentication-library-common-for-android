package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.start

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiErrorResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.Attribute
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartErrorCodes
import com.microsoft.identity.common.java.exception.ClientException

data class SignUpStartErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: SignUpStartErrorCodes?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("signup_token")val signupToken: String?,
    @SerializedName("unverified_attributes") val unverifiedAttributes: List<Attribute>?,
    @SerializedName("invalid_attributes") val invalidAttributes: String?,
    @SerializedName("challenge_type") val challengeType: NativeAuthChallengeType?
) : IApiErrorResponse {
    private val TAG = SignUpStartErrorResponse::class.java.simpleName

    override fun getError() = errorCode.toString()

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (errorCode == null) {
            throw ClientException("SignUpStartErrorResponse.error=$errorCode can't be null or invalid in error state")
        }

        if (errorCode == SignUpStartErrorCodes.VALIDATION_FAILED) {
            if (signupToken.isNullOrBlank()) {
                throw ClientException("SignUpStartErrorResponse.signupToken can't be null in error state when error is validation_failed or verification_required or attributes_required")
            }
            if (invalidAttributes == null) {
                throw ClientException("SignUpStartErrorResponse.invalidAttributes can't be null in error state when error is validation_failed")
            }
        }

        if (errorCode == SignUpStartErrorCodes.VERIFICATION_REQUIRED) {
            if (signupToken.isNullOrBlank()) {
                throw ClientException("SignUpStartErrorResponse.signupToken can't be null in error state when error is validation_failed or verification_required or attributes_required")
            }
            if (unverifiedAttributes == null) {
                throw ClientException("SignUpStartErrorResponse.unverifiedAttributes can't be null in error state when error is verification_required")
            }
        }

        if (errorCode == SignUpStartErrorCodes.VALIDATION_FAILED) {
            if (signupToken.isNullOrBlank()) {
                throw ClientException("SignUpContinueErrorResponse.signupToken can't be null in error state when error is validation_failed or verification_required or attributes_required")
            }
            // TODO: there is no required_attributes in the doc design
//            if (requiredAttributes == null) {
//                throw ClientException("SignUpStartErrorResponse.requiredAttributes can't be null in error state when error is attributes_required")
//            }
        }
    }

    override fun validateOptionalFields() {}
}
