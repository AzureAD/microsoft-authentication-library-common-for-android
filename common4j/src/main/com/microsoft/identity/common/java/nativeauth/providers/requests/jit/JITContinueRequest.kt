package com.microsoft.identity.common.java.nativeauth.providers.requests.jit

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the register/continue endpoint, and provides a create() function to instantiate the request using the provided parameters.
 */
data class JITContinueRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthJITContinueRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         * The clientId, continuation token will be mapped to the NativeAuthJITContinueRequestParameters object.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            continuationToken: String,
            grantType: String,
            oob: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): JITContinueRequest {
            // Check for empty Strings and empty Maps
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(grantType, "grantType")
            ArgUtils.validateNonNullArg(oob, "oob")

            return JITContinueRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthJITContinueRequestParameters(
                    clientId = clientId,
                    continuationToken = continuationToken,
                    grantType = grantType,
                    oob = oob
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "JITContinueRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "JITContinueRequest()"

    /**
     * NativeAuthJITContinueRequestParameters represents the request parameters sent as part of
     * /register/continue API call
     */
    data class NativeAuthJITContinueRequestParameters(
        @SerializedName("client_id") override val clientId: String,
        @SerializedName("continuation_token") val continuationToken: String,
        @SerializedName("grant_type") val grantType: String,
        @SerializedName("oob") val oob: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthJITContinueRequestParameters(clientId=$clientId)"

        override fun toString(): String = toUnsanitizedString()
    }
}