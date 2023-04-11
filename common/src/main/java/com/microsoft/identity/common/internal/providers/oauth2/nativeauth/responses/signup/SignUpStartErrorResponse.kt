package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin.exceptions.ErrorCodes
import com.microsoft.identity.common.java.exception.ClientException

data class SignUpStartErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: String?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("verify_attributes") val verifyAttributes: List<Attribute>?,
    @SerializedName("invalid_attributes") val invalidAttributes: String?,
    @SerializedName("challenge_type") val challengeType: String?,
    @SerializedName("signup_token")val signupToken: String?
) : com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiErrorResponse {
    override fun getError() = errorCode

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (error == null || error!!.isEmpty()) {
            throw ClientException("SignUpStartErrorResponse.error can't be null in error state")
        }
        if (getErrorDescription() == null || getErrorDescription()!!.isEmpty()) {
            throw ClientException("SignUpStartErrorResponse.errorDescription can't be null in error state")
        }
        if (errorCode == ErrorCodes.VERIFICATION_REQUIRED) {
            if (verifyAttributes == null) {
                throw ClientException("verifyAttributes can't be null in error state when error is verification_required")
            }
            if (signupToken == null) {
                throw ClientException("signupToken can't be null in error state when error is verification_required")
            }
        }
        if (errorCode == ErrorCodes.VALIDATION_FAILED) {
            if (invalidAttributes == null) {
                throw ClientException("invalidAttributes can't be null in error state when error is validation_failed")
            }
        }
    }

    override fun validateOptionalFields() {
        // No optional fields
    }
}
