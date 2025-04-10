package com.microsoft.identity.common.java.nativeauth.providers.requests.jit

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the register/challenge endpoint, and provides a create() function to instantiate the request using the provided parameters.
 */
data class JITChallengeRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The clientId, continuation token, and challengeType will be mapped to the NativeAuthJITChallengeRequestParameters object.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            continuationToken: String,
            challengeType: String,
            challengeTarget: String,
            challengeChannel: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): JITChallengeRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(challengeType, "challengeType")
            ArgUtils.validateNonNullArg(challengeTarget, "challengeTarget")
            ArgUtils.validateNonNullArg(challengeChannel, "challengeChannel")

            return JITChallengeRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthJITChallengeRequestParameters(
                    clientId = clientId,
                    continuationToken = continuationToken,
                    challengeType = challengeType,
                    challengeTarget = challengeTarget,
                    challengeChannel = challengeChannel
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "JITChallengeRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "JITChallengeRequest()"

    /**
     * NativeAuthJITChallengeRequestParameters represents the request parameters sent as part of
     * /register/challenge API call
     */
    data class NativeAuthJITChallengeRequestParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("continuation_token") val continuationToken: String,
        @SerializedName("challenge_type") val challengeType: String,
        @SerializedName("challenge_target") val challengeTarget: String,
        @SerializedName("challenge_channel") val challengeChannel: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthJITChallengeRequestParameters(clientId=$clientId)"

        override fun toString(): String = toUnsanitizedString()
    }
}