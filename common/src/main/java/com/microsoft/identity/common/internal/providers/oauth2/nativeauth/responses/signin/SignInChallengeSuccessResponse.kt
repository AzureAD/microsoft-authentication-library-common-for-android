package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.IApiSuccessResponse
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.oauth2.ISuccessResponse
import com.microsoft.identity.common.logging.Logger

class SignInChallengeSuccessResponse(
    @SerializedName("credential_token") val credentialToken: String?,
    @SerializedName("challenge_type") val challengeType: String?,
    @SerializedName("binding_method") val bindingMethod: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("display_type") val displayType: String?,
    @SerializedName("code_length") val codeLength: String?,
    @SerializedName("interval") val interval: Int?
) : ISuccessResponse, IApiSuccessResponse {
    private val TAG = SignInChallengeSuccessResponse::class.java.simpleName

    @Transient
    private var extraParameters: Iterable<Map.Entry<String, String>>? = null

    override fun getExtraParameters(): Iterable<Map.Entry<String, String>>? {
        return extraParameters
    }

    override fun setExtraParameters(params: Iterable<Map.Entry<String, String>>) {
        this.extraParameters = params
    }

    override fun validateRequiredFields() {
        if (challengeType.isNullOrEmpty()) {
            throw ClientException("SignInChallengeSuccessResponse challengeType can't be null or empty")
        }
    }

    override fun validateOptionalFields() {
        if (credentialToken.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInChallengeSuccessResponse credentialToken is null or empty")
        }
        if (bindingMethod.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInChallengeSuccessResponse bindingMethod is null or empty")
        }
        if (displayName.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInChallengeSuccessResponse displayName is null or empty")
        }
        if (displayType.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInChallengeSuccessResponse displayType is null or empty")
        }
        if (codeLength.isNullOrEmpty()) {
            Logger.verbose(TAG, "SignInChallengeSuccessResponse codeLength is null or empty")
        }
    }
}
