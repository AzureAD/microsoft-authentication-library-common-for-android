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
 * Represents a request to the HAL-resolved entry endpoint of a Native Auth V2 sign-up flow
 * (`signup/start`), resolved via the [com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation.SIGN_UP]
 * relation. Unlike sign-in and reset-password, sign-up's entry call carries no username: the
 * identifier is supplied later as an attribute via `submitattributes`, so the body is just the
 * `continuationToken`.
 */
data class NativeAuthV2SignUpStartRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2SignUpStartRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            continuationToken: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2SignUpStartRequest {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2SignUpStartRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2SignUpStartRequestParameters(
                    continuationToken = continuationToken
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2SignUpStartRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2SignUpStartRequest()"

    /**
     * NativeAuthV2SignUpStartRequestParameters represents the JSON request body sent to a Native
     * Auth V2 sign-up flow's entry endpoint. [continuationToken] is never included in either
     * string representation.
     */
    data class NativeAuthV2SignUpStartRequestParameters(
        val continuationToken: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthV2SignUpStartRequestParameters()"

        override fun toString(): String = toUnsanitizedString()
    }
}
