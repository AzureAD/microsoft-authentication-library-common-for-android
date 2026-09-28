//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers.requests.v2

import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import java.net.URL

/**
 * Represents a request to the HAL-resolved entry endpoint of a Native Auth V2 flow (for example,
 * reset-password `/start`), and provides a create() function to instantiate the request using the
 * provided parameters. The request URL is resolved from the flow's server-provided entry link
 * relation by [com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider],
 * so no endpoint suffix constant lives here. The body is JSON, using the wire's own camelCase
 * field names.
 */
data class NativeAuthV2EntryRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2EntryRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            username: String,
            continuationToken: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2EntryRequest {
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(username, "username")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2EntryRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2EntryRequestParameters(
                    clientId = clientId,
                    username = username,
                    continuationToken = continuationToken
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2EntryRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2EntryRequest()"

    /**
     * NativeAuthV2EntryRequestParameters represents the JSON request body sent to a Native Auth V2
     * flow's entry endpoint. [clientId] is not part of the wire body — the resolved href already
     * scopes the request — so it is marked [Transient] to keep it out of the serialized JSON while
     * still satisfying [NativeAuthRequestParameters]. [username] and [continuationToken] are never
     * included in either string representation, matching the existing V1
     * `ResetPasswordStartRequest` convention of omitting username entirely.
     */
    data class NativeAuthV2EntryRequestParameters(
        @Transient override val clientId: String,
        val username: String,
        val continuationToken: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthV2EntryRequestParameters(clientId=$clientId)"

        override fun toString(): String = toUnsanitizedString()
    }
}
