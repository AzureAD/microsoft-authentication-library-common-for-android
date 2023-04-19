package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SignUpStartResponse(
    @SerializedName("signup_token") val signupToken: String?
) : ISuccessResponse,
    com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse {
    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (signupToken.isNullOrBlank()) {
            throw ClientException("SignUpStartSuccessResponse.signupToken can't be null or empty in success state")
        }
    }

    override fun validateOptionalFields() {}
}
