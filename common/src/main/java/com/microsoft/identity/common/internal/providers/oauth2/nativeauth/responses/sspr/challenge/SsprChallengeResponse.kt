package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.sspr.challenge

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.commands.parameters.SsprChallengeCommandParameters
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthBindingMethod
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.NativeAuthDisplayType
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse
import com.microsoft.identity.common.logging.Logger

class SsprChallengeResponse(
    @SerializedName("password_reset_token")val passwordResetToken: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("display_type") val displayType: NativeAuthDisplayType?,
    @SerializedName("code_length") val codeLength: String?,
    @SerializedName("challenge_type") val challengeType: NativeAuthChallengeType?,
    @SerializedName("binding_method") val bindingMethod: NativeAuthBindingMethod?
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
        if (passwordResetToken.isNullOrBlank()) {
            throw ClientException("SsprSsprResponse.passwordResetToken can't be null or empty in success state")
        }
        if (challengeType == null || challengeType == NativeAuthChallengeType.UNKNOWN) {
            throw ClientException("SsprSsprResponse.challengeType can't be null or empty in success state")
        }
        if (bindingMethod == null || bindingMethod == NativeAuthBindingMethod.UNKNOWN) {
            throw ClientException("SsprSsprResponse.bindingMethod can't be null or empty in success state")
        }
        if (!checkChallengeTypeValidity(challengeType)) {
            throw ClientException("SsprSsprResponse.challengeType=$challengeType is invalid.")
        }
    }

    override fun validateOptionalFields() {
        if (displayType != null && !checkDisplayTypeValidity(displayType)) {
            Logger.verbose(SsprChallengeCommandParameters::class.java.simpleName, "SsprChallengeErrorResponse.displayType $displayType is invalid in success state")
        }
    }

    private fun checkChallengeTypeValidity(challengeType: NativeAuthChallengeType?): Boolean {
        return NativeAuthChallengeType.values().contains(challengeType)
    }

    private fun checkDisplayTypeValidity(displayType: NativeAuthDisplayType?): Boolean {
        return NativeAuthDisplayType.values().contains(displayType)
    }
}
