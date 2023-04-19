package com.microsoft.identity.common.java.providers.nativeauth.requests.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

data class SignUpChallengeRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpStartParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            signUpToken: String,
            clientId: String,
            challengeType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignUpChallengeRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(signUpToken, "signUpToken")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeTypes")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignUpChallengeRequest(
                parameters = NativeAuthRequestSignUpStartParameters(
                    signUpToken = signUpToken,
                    challengeType = challengeType,
                    clientId = clientId
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignUpStartParameters(
        @SerializedName("signup_token") val signUpToken: String,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String
    ) : NativeAuthRequestParameters()
}
