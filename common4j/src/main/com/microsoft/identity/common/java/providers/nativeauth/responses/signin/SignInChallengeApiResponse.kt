package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isOOB
import com.microsoft.identity.common.java.util.isPassword
import com.microsoft.identity.common.java.util.isRedirect
import java.net.HttpURLConnection

// TODO are these fields considered PII?
data class SignInChallengeApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @Expose @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @Expose @SerializedName("code_length") val codeLength: Int?,
    @Expose @SerializedName("interval") val interval: Int?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
): IApiResponse(statusCode) {
    fun toResult(): SignInChallengeApiResult {
        return if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            if (error == "invalid_grant") {
                // TODO advanced error handling
                SignInChallengeApiResult.UnknownError(error, errorDescription)
            } else {
                // TODO log the API response, in a PII-safe way
                SignInChallengeApiResult.UnknownError(error, errorDescription)
            }
        } else {
            if (challengeType.isRedirect()) {
                SignInChallengeApiResult.Redirect
            }
            else if (challengeType.isOOB()) {
                if (challengeTargetLabel.isNullOrBlank()) {
                    throw ClientException("SignInChallengeApiResponse challengeTargetLabel can't be null or empty in oob state")
                }
                if (challengeChannel.isNullOrBlank()) {
                    throw ClientException("SignInChallengeApiResponse challengeChannel can't be null or empty in oob state")
                }
                if (codeLength == null) {
                    throw ClientException("SignInChallengeApiResponse codeLength can't be null or empty in oob state")
                }
                if (credentialToken.isNullOrBlank()) {
                    throw ClientException("SignInChallengeApiResponse credentialToken can't be null or empty in oob state")
                }
                SignInChallengeApiResult.OOBRequired(
                    credentialToken = credentialToken,
                    challengeTargetLabel = challengeTargetLabel,
                    codeLength = codeLength,
                    challengeChannel = challengeChannel
                )
            }
            else if (challengeType.isPassword()) {
                if (credentialToken.isNullOrBlank()) {
                    throw ClientException("SignInChallengeApiResponse credentialToken can't be null or empty in password state")
                }
                SignInChallengeApiResult.PasswordRequired(credentialToken)
            }
            else {
                SignInChallengeApiResult.UnknownError(error, errorDescription)
            }
        }
    }
}