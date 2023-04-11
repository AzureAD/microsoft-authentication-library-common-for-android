package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SignInTokenSuccessResponse(
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("scope") val scope: String?,
    @SerializedName("expires_in") val expiresIn: Long?,
    @SerializedName("ext_expires_in") val extExpiresIn: Long?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("id_token") val idToken: String?,
) : ISuccessResponse, IApiSuccessResponse {
    private val TAG = SignInTokenSuccessResponse::class.java.simpleName

    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (tokenType.isNullOrEmpty()) {
            throw ClientException("SignInTokenSuccessResponse tokenType can't be null or empty in success state")
        }
        if (scope.isNullOrEmpty()) {
            throw ClientException("SignInTokenSuccessResponse scope can't be null or empty in success state")
        }
        if (accessToken.isNullOrEmpty()) {
            throw ClientException("SignInTokenSuccessResponse accessToken can't be null or empty in success state")
        }
        if (refreshToken.isNullOrEmpty()) {
            throw ClientException("SignInTokenSuccessResponse refreshToken can't be null or empty in success state")
        }
        if (idToken.isNullOrEmpty()) {
            throw ClientException("SignInTokenSuccessResponse idToken can't be null or empty in success state")
        }
    }

    override fun validateOptionalFields() {
        // No optional fields
    }
}
