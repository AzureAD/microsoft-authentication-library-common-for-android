package com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.providers.nativeauth.responses.NativeAuthChallengeType
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse

class SsprStartResponse(
    @SerializedName("password_reset_token") val passwordResetToken: String?,
    @SerializedName("challenge_type") val challengeType: NativeAuthChallengeType?
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
        if (challengeType == NativeAuthChallengeType.UNKNOWN) {
            throw ClientException("SsprStartResponse does not return unknown challengeType in success state")
        }

        if (challengeType == null && passwordResetToken.isNullOrEmpty()) {
            throw ClientException("SsprStartResponse does not return valid challengeType or passwordResetToken in success state")
        }
    }

    override fun validateOptionalFields() {
        if (challengeType != null && !checkChallengeTypeValidity(challengeType)) {
            Logger.verbose(SsprStartCommandParameters::class.java.simpleName, "SsprPollCompletionErrorResponse.challengeType=$challengeType is invalid in succeeded state")
        }
    }

    private fun checkChallengeTypeValidity(challengeType: NativeAuthChallengeType): Boolean {
        return challengeType == NativeAuthChallengeType.REDIRECT
    }
}
