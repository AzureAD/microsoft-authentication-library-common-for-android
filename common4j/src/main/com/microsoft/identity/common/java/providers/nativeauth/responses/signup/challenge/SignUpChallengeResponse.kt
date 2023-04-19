package com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthBindingMethod
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthDisplayType
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SignUpChallengeResponse(
    @SerializedName("signup_token") val signupToken: String?,
    @SerializedName("challenge_type") val challengeType: NativeAuthChallengeType?,
    @SerializedName("code_length") val codeLength: Int?,
    @SerializedName("binding_method") val bindingMethod: NativeAuthBindingMethod?,
    @SerializedName("interval") val interval: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("display_type") val displayType: NativeAuthDisplayType?
) : ISuccessResponse, IApiSuccessResponse {

    private val TAG = SignUpChallengeResponse::class.java.simpleName

    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (signupToken.isNullOrBlank() && challengeType == null) {
            throw ClientException("SignUpChallengeSuccessResponse.signupToken and SignUpChallengeSuccessResponse.challengeType can't be null or empty in success state")
        }
        if (challengeType == null) {
            throw ClientException("SignUpChallengeSuccessResponse.challengeType=$challengeType can't be null or empty or invalid in success state")
        }
    }

    override fun validateOptionalFields() {
        if (codeLength == null) {
            Logger.verbose(TAG, "SignUpChallengeSuccessResponse.codeLength is null")
        }
        if (interval.isNullOrBlank()) {
            Logger.verbose(TAG, "SignUpChallengeSuccessResponse.interval is null")
        }

        if (displayType == null) {
            Logger.verbose(TAG, "SignUpChallengeSuccessResponse.displayType=$displayType is null or invalid")
        }

        if (bindingMethod != NativeAuthBindingMethod.PROMPT) {
            Logger.verbose(TAG, "SignUpChallengeSuccessResponse.bindingMethod=$bindingMethod should be prompt.")
        }
    }
}
