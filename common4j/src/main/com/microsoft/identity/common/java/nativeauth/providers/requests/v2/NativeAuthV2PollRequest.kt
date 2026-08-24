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
 * Represents a request to a Native Auth V2 flow's HAL-resolved `poll` endpoint, and provides a
 * create() function to instantiate the request using the provided parameters.
 */
data class NativeAuthV2PollRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2PollRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            continuationToken: String,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2PollRequest {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2PollRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2PollRequestParameters(
                    continuationToken = continuationToken
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2PollRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2PollRequest()"

    /**
     * NativeAuthV2PollRequestParameters represents the JSON request body sent to a Native Auth V2
     * flow's `poll` endpoint.
     */
    data class NativeAuthV2PollRequestParameters(
        val continuationToken: String
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String = "NativeAuthV2PollRequestParameters()"

        override fun toString(): String = toUnsanitizedString()
    }
}
