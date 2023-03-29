package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

data class SignUpChallengeRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpStartParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            signUpToken: String,
            clientId: String,
            challengeTypes: String, // TODO hardcoded for now, but will be made part of SDK config & initialisation ticket
            requestUrl: String,
            headers: Map<String, String?>
        ): SignUpChallengeRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(signUpToken, "signUpToken")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeTypes, "challengeTypes")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignUpChallengeRequest(
                parameters = NativeAuthRequestSignUpStartParameters(
                    signUpToken = signUpToken,
                    challengeTypes = challengeTypes,
                    clientId = clientId
                ),
                requestUrl = URL(requestUrl),
                headers = headers,
            )
        }
    }

    data class NativeAuthRequestSignUpStartParameters(
        @SerializedName("signup_token") val signUpToken: String,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeTypes: String,
    ) : NativeAuthRequestParameters()
}
