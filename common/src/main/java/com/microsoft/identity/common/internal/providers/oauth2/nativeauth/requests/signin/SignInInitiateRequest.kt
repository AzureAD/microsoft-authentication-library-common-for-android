package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signin

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

data class SignInInitiateRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignInInitiateParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            username: String,
            clientId: String,
            challengeType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SignInInitiateRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignInInitiateRequest(
                parameters = NativeAuthRequestSignInInitiateParameters(
                    username = username,
                    challengeType = challengeType,
                    clientId = clientId
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignInInitiateParameters(
        val username: String,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String
    ) : NativeAuthRequestParameters()
}
