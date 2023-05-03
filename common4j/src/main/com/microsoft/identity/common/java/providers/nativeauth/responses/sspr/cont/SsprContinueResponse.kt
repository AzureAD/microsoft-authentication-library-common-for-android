package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprContinueCommandParameters
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SsprContinueResponse(
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("password_submit_token") val passwordSubmitToken: String?,
    @SerializedName("error") val error: String?
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
        if (passwordSubmitToken.isNullOrEmpty()) {
            throw ClientException("SsprContinueResponse.passwordSubmitToken can't be null or empty in success state")
        }
    }

    override fun validateOptionalFields() {
        if (expiresIn != null) {
            if (expiresIn > 600) {
                Logger.verbose(SsprContinueCommandParameters::class.java.simpleName, "SsprContinueErrorResponse.expiresIn=$expiresIn is greater than 600 in success state")
            }
        }

        if (error != null) {
            if (error != "verification_required") {
                Logger.verbose(SsprContinueCommandParameters::class.java.simpleName, "SsprContinueErrorResponse.error=$error is invalid in success state")
            }
        }
    }
}
