package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SignUpContinueResponse(
    @SerializedName("signup_token")val signupToken: String?,
    @SerializedName("expires_in") val expiresIn: String?
) : ISuccessResponse, IApiSuccessResponse {

    private val TAG = SignUpContinueResponse::class.java.simpleName

    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {}

    override fun validateOptionalFields() {
        if (expiresIn.isNullOrBlank()) {
            Logger.verbose(TAG, "SignUpContinueSuccessResponse.expiresIn is null")
        }
    }
}
