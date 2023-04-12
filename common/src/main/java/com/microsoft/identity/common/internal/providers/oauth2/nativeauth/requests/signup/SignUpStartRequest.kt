package com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.signup

import androidx.annotation.VisibleForTesting
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.internal.commands.parameters.UserAttributes
import com.microsoft.identity.common.internal.commands.parameters.toJsonString
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.internal.util.ArgUtils
import java.net.URL

data class SignUpStartRequest @VisibleForTesting private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpStartParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            username: String,
            password: String? = null,
            attributes: UserAttributes? = null,
            clientId: String,
            challengeType: String, // TODO hardcoded for now, but will be made part of SDK config & initialisation ticket
            requestUrl: String,
            headers: Map<String, String?>
        ): SignUpStartRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SignUpStartRequest(
                parameters = NativeAuthRequestSignUpStartParameters(
                    username = username,
                    password = password,
                    attributes = attributes?.toJsonString(),
                    challengeType = challengeType,
                    clientId = clientId
                ),
                requestUrl = URL(requestUrl),
                headers = headers
            )
        }
    }

    data class NativeAuthRequestSignUpStartParameters(
        val username: String,
        val password: String?,
        val attributes: String?,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String
    ) : NativeAuthRequestParameters()
}
