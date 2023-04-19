package com.microsoft.identity.common.java.providers.nativeauth.requests.signin

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

data class SignInChallengeRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignInChallengeParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            clientId: String,
            credentialToken: String,
            challengeType: String? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInChallengeRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(credentialToken, "credentialToken")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignInChallengeRequest(
                parameters = NativeAuthRequestSignInChallengeParameters(
                    clientId = clientId,
                    credentialToken = credentialToken,
                    challengeType = challengeType
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignInChallengeParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("credential_token") val credentialToken: String,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters()
}
