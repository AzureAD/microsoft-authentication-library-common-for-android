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
 * Represents a request to a Native Auth V2 sign-in flow's HAL-resolved password `verify` endpoint,
 * and provides a create() function to instantiate the request using the provided parameters.
 *
 * The request URL is resolved from the `verify` relation the password-method challenge returned,
 * never from an endpoint path built out of a method ID. The body is JSON, using the wire's own
 * camelCase field names: `password` and `continuationToken`.
 */
data class NativeAuthV2PasswordVerifyRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2PasswordVerifyRequestParameters
) : NativeAuthRequest() {

    companion object {
        /**
         * Returns a request object using the provided parameters. [password] is retained as the
         * caller's own array, not copied, so the interactor's `finally` block clears the same
         * buffer it passed in.
         *
         * Parameters that are null or empty will throw a ClientException.
         * @see com.microsoft.identity.common.java.exception.ClientException
         */
        fun create(
            continuationToken: String,
            password: CharArray,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2PasswordVerifyRequest {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(password, "password")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2PasswordVerifyRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2PasswordVerifyRequestParameters(
                    continuationToken = continuationToken,
                    password = password
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2PasswordVerifyRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2PasswordVerifyRequest()"

    /**
     * NativeAuthV2PasswordVerifyRequestParameters represents the JSON request body sent to a
     * Native Auth V2 sign-in flow's password `verify` endpoint. Neither [continuationToken] nor
     * [password] appears in either string representation. [password] is annotated with
     * [CharArrayJsonAdapter] so Gson serializes it as the service's password string rather than a
     * JSON array of characters.
     */
    data class NativeAuthV2PasswordVerifyRequestParameters(
        val continuationToken: String,
        @JsonAdapter(CharArrayJsonAdapter::class) val password: CharArray
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthV2PasswordVerifyRequestParameters()"

        override fun toString(): String = toUnsanitizedString()
    }
}
