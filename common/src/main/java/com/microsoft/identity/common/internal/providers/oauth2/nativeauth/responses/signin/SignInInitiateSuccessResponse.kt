package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SignInInitiateSuccessResponse(
    @SerializedName("credential_token") val credentialToken: String?,
    @SerializedName("challenge_type") val challengeType: String?
) : ISuccessResponse, IApiSuccessResponse {
    private val TAG = SignInInitiateSuccessResponse::class.java.simpleName

    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (credentialToken.isNullOrEmpty() && challengeType.isNullOrEmpty()) {
            throw ClientException("SignInInitiateSuccessResponse credentialToken and challengeType both can't be null or empty in success state")
        }
        if (!credentialToken.isNullOrEmpty() && !challengeType.isNullOrEmpty()) {
            throw ClientException("SignInInitiateSuccessResponse credentialToken and challengeType both can't be present in success state")
        }
    }

    override fun validateOptionalFields() {
        // No optional fields
    }
}
