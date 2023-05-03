package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiErrorResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.Attribute
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueErrorCodes

data class SignUpContinueErrorResponse(
    var statusCode: Int,
    @SerializedName("error") private val errorCode: SignUpContinueErrorCodes?,
    @SerializedName("error_description") private val errorDescription: String?,
    @SerializedName("signup_token")val signupToken: String?,
    @SerializedName("verify_attributes") val verifyAttributes: List<Attribute>?,
    @SerializedName("invalid_attributes") val invalidAttributes: String?,
    @SerializedName("required_attributes") val requiredAttributes: List<Attribute>?
) : IApiErrorResponse {

    private val TAG = SignUpContinueErrorResponse::class.java.simpleName

    override fun getError() = errorCode.toString()

    override fun getErrorDescription() = errorDescription

    override fun validateRequiredFields() {
        if (errorCode == null) {
            throw ClientException("SignUpContinueErrorResponse.erro=$errorCode can't be null or invalid in error state")
        }
        if (errorCode == SignUpContinueErrorCodes.VALIDATION_FAILED) {
            if (signupToken.isNullOrBlank()) {
                throw ClientException("SignUpContinueErrorResponse.signupToken can't be null in error state when error is validation_failed or verification_required or attributes_required")
            }
            if (invalidAttributes == null) {
                throw ClientException("SignUpContinueErrorResponse.invalidAttributes can't be null in error state when error is validation_failed")
            }
        }

        if (errorCode == SignUpContinueErrorCodes.VERIFICATION_REQUIRED) {
            if (signupToken.isNullOrBlank()) {
                throw ClientException("SignUpContinueErrorResponse.signupToken can't be null in error state when error is validation_failed or verification_required or attributes_required")
            }
            if (verifyAttributes == null) {
                throw ClientException("SignUpContinueErrorResponse.verifyAttributes can't be null in error state when error is verification_required")
            }
        }

        if (errorCode == SignUpContinueErrorCodes.VALIDATION_FAILED) {
            if (signupToken.isNullOrBlank()) {
                throw ClientException("SignUpContinueErrorResponse.signupToken can't be null in error state when error is validation_failed or verification_required or attributes_required")
            }
            if (requiredAttributes == null) {
                throw ClientException("SignUpContinueErrorResponse.requiredAttributes can't be null in error state when error is attributes_required")
            }
        }
    }

    override fun validateOptionalFields() {}
}
