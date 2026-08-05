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

import com.google.gson.annotations.JsonAdapter
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import com.microsoft.identity.common.java.util.CharArrayJsonAdapter
import java.net.URL

/**
 * Represents a request to a Native Auth V2 flow's HAL-resolved `update` endpoint (submitting a new
 * password), and provides a create() function to instantiate the request using the provided
 * parameters. The request URL is resolved from the flow's server-provided `update` link relation
 * by [com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider]. The
 * body is JSON, using the wire's own camelCase field names. This request is sent with HTTP PUT,
 * verified explicitly by the calling interactor rather than represented as a value on this class.
 */
data class NativeAuthV2UpdatePasswordRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2UpdatePasswordRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters.
         * The request URL and headers passed will be set directly. [newPassword] is retained as
         * the caller's own array, not copied, so the interactor's `finally` block can clear the
         * same buffer it passed in.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            clientId: String,
            continuationToken: String,
            newPassword: CharArray,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2UpdatePasswordRequest {
            ArgUtils.validateNonNullArg(clientId, "clientId")
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(newPassword, "newPassword")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2UpdatePasswordRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2UpdatePasswordRequestParameters(
                    clientId = clientId,
                    continuationToken = continuationToken,
                    newPassword = newPassword
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2UpdatePasswordRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2UpdatePasswordRequest()"

    /**
     * NativeAuthV2UpdatePasswordRequestParameters represents the JSON request body sent to a
     * Native Auth V2 flow's `update` endpoint. [clientId] is not part of the wire body — the
     * resolved href already scopes the request — so it is marked [Transient] to keep it out of the
     * serialized JSON while still satisfying [NativeAuthRequestParameters]. [continuationToken] and
     * [newPassword] are never included in either string representation. [newPassword] is annotated
     * with [CharArrayJsonAdapter] so Gson serializes it as the service's password string rather
     * than a JSON array of characters.
     */
    data class NativeAuthV2UpdatePasswordRequestParameters(
        @Transient override val clientId: String,
        val continuationToken: String,
        @JsonAdapter(CharArrayJsonAdapter::class) val newPassword: CharArray
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthV2UpdatePasswordRequestParameters(clientId=$clientId)"

        override fun toString(): String = toUnsanitizedString()
    }
}
