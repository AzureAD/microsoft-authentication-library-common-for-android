package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.sspr

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

class SsprChallengeRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            clientId: String,
            passwordResetToken: String,
            challengeType: String?,
            requestUrl: String,
            headers: Map<String, String?>
        ): SsprChallengeRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(passwordResetToken, "passwordResetToken")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SsprChallengeRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthSsprChallengeRequestBody(
                    clientId = clientId,
                    passwordResetToken = passwordResetToken,
                    challengeType = challengeType
                )
            )
        }
    }

    data class NativeAuthSsprChallengeRequestBody(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("password_reset_token") val passwordResetToken: String,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters()
}
