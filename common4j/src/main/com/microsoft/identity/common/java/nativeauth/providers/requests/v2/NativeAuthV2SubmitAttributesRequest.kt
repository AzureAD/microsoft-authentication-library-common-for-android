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
 * Represents a request to a Native Auth V2 sign-up flow's HAL-resolved `submitattributes`
 * endpoint, resolved via the
 * [com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation.SUBMIT_ATTRIBUTES]
 * relation. The body carries the `continuationToken` and an `attributes` object mapping each
 * attribute's wire name to its value (for example `email` or `password`).
 */
data class NativeAuthV2SubmitAttributesRequest private constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: NativeAuthV2SubmitAttributesRequestParameters
) : NativeAuthRequest() {

    companion object {
        fun create(
            continuationToken: String,
            attributes: Map<String, String>,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2SubmitAttributesRequest {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            ArgUtils.validateNonNullArg(attributes, "attributes")
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2SubmitAttributesRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2SubmitAttributesRequestParameters(
                    continuationToken = continuationToken,
                    attributes = attributes
                )
            )
        }
    }

    override fun toUnsanitizedString(): String = "NativeAuthV2SubmitAttributesRequest(requestUrl=$requestUrl, headers=$headers, parameters=$parameters)"

    override fun toString(): String = "NativeAuthV2SubmitAttributesRequest()"

    /**
     * NativeAuthV2SubmitAttributesRequestParameters represents the JSON request body sent to a
     * Native Auth V2 sign-up flow's `submitattributes` endpoint. Neither [continuationToken] nor
     * any attribute *value* is ever included in a string representation, since an attribute value
     * may be a password or other PII; only the attribute *names* are surfaced (in the unsanitized
     * form) for diagnostics.
     */
    data class NativeAuthV2SubmitAttributesRequestParameters(
        val continuationToken: String,
        val attributes: Map<String, String>
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2SubmitAttributesRequestParameters(attributeNames=${attributes.keys})"

        override fun toString(): String = "NativeAuthV2SubmitAttributesRequestParameters()"
    }
}
