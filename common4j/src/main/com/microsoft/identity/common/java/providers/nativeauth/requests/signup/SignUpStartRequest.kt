package com.microsoft.identity.common.java.providers.nativeauth.requests.signup

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

data class SignUpStartRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestSignUpStartParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
                username: String,
                password: String? = null,
                attributes: Map<String, String>? = null,
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
//                    attributes = attributes?.toJsonString(), // TODO
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
        val attributes: String? = null,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String
    ) : NativeAuthRequestParameters()
}
