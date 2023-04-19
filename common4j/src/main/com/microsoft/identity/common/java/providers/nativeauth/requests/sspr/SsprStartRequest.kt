package com.microsoft.identity.common.java.providers.nativeauth.requests.sspr

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

data class SsprStartRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override var parameters: NativeAuthRequestSsprStartParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            clientId: String,
            username: String,
            challengeType: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): SsprStartRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return SsprStartRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthRequestSsprStartParameters(
                    clientId = clientId,
                    username = username,
                    challengeType = challengeType
                )
            )
        }
    }

    data class NativeAuthRequestSsprStartParameters(
        val username: String,
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("challenge_type") val challengeType: String?
    ) : NativeAuthRequestParameters()
}
