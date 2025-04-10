package com.microsoft.identity.common.java.nativeauth.providers.requests.jit

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the register/introspect endpoint, and provides a create() function to instantiate the request using the provided parameters.
 */
data class JITIntrospectRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The clientId, continuation token, and challengeType will be mapped to the NativeAuthJITIntrospectRequestParameters object.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            continuationToken: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): JITIntrospectRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")

            return JITIntrospectRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthJITIntrospectRequestParameters(
                    clientId = clientId,
                    continuationToken = continuationToken
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "JITIntrospectRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "JITIntrospectRequest()"

    /**
     * NativeAuthJITIntrospectRequestParameters represents the request parameters sent as part of
     * /register/introspect API call
     */
    data class NativeAuthJITIntrospectRequestParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("continuation_token") val continuationToken: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthJITIntrospectRequestParameters(clientId=$clientId)"

        override fun toString(): String = toUnsanitizedString()
    }
}