package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SsprSubmitResponse(
    @SerializedName("password_reset_token")val passwordResetToken: String?,
    @SerializedName("poll_interval") val pollInterval: Int?
) : ISuccessResponse, IApiSuccessResponse {
    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (passwordResetToken.isNullOrEmpty()) {
            throw ClientException("SsprSubmitResponse.passwordResetToken can't be null or empty in success state")
        }
        if (pollInterval == null) {
            throw ClientException("SsprSubmitResponse.pollInterval can't be null in success state")
        }
        if (pollInterval <= 0) {
            throw ClientException("SsprSubmitResponse.pollInterval=$pollInterval can't be less than or equal to 0 in success state")
        }
    }

    override fun validateOptionalFields() {}
}
