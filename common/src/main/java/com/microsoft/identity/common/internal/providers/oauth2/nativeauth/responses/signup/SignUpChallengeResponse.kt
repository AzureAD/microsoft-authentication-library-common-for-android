package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SignUpChallengeResponse(
    @SerializedName("signup_token") val signupToken: String?,
    @SerializedName("challenge_type") val challengeType: String?,
    @SerializedName("code_length") val codeLength: Int?,
    @SerializedName("binding_method") val bindingMethod: String?,
    @SerializedName("interval") val interval: String?,
    @SerializedName("display_name") val displayName: String?
) : ISuccessResponse,
    com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiSuccessResponse {

    val TAG = SignUpChallengeResponse::class.java.simpleName

    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (this.signupToken == null || this.signupToken.isEmpty()) {
            throw ClientException("SignUpChallengeResponse.signupToken can't be null or empty in success state")
        }
        if (this.challengeType == null) {
            throw ClientException("SignUpChallengeResponse.challengeType can't be null or empty in success state")
        }
    }

    override fun validateOptionalFields() {
        if (this.displayName == null) {
            Logger.warn(TAG, "SignUpChallengeResponse.displayName is null")
        }
        if (this.codeLength == null) {
            Logger.warn(TAG, "SignUpChallengeResponse.codeLength is null")
        }
        if (this.interval == null) {
            Logger.warn(TAG, "SignUpChallengeResponse.interval is null")
        }
        if (this.bindingMethod == null) {
            Logger.warn(TAG, "SignUpChallengeResponse.bindingMethod is null")
        }
    }
}
