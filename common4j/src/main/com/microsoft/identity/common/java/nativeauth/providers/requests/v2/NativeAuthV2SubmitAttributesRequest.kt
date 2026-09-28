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

import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.util.ArgUtils
import com.microsoft.identity.common.java.util.CharArrayJsonAdapter
import java.io.IOException
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
        private const val PASSWORD = "password"

        fun create(
            continuationToken: String,
            attributes: Map<String, String>,
            password: CharArray? = null,
            requestUrl: String,
            headers: Map<String, String?>
        ): NativeAuthV2SubmitAttributesRequest {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            if (password == null || password.isEmpty()) {
                ArgUtils.validateNonNullArg(attributes, "attributes")
            }
            if (attributes.keys.any { it.equals(PASSWORD, ignoreCase = true) }) {
                throw ClientException(
                    PASSWORD,
                    "password must not be included in attributes"
                )
            }
            ArgUtils.validateNonNullArg(requestUrl, "requestUrl")
            ArgUtils.validateNonNullArg(headers, "headers")

            return NativeAuthV2SubmitAttributesRequest(
                requestUrl = URL(requestUrl),
                headers = headers,
                parameters = NativeAuthV2SubmitAttributesRequestParameters(
                    continuationToken = continuationToken,
                    attributes = NativeAuthV2SubmitAttributes(
                        values = attributes,
                        password = password
                    )
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
        val attributes: NativeAuthV2SubmitAttributes
    ) : NativeAuthRequestParameters() {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2SubmitAttributesRequestParameters(attributeNames=${attributes.names})"

        override fun toString(): String = "NativeAuthV2SubmitAttributesRequestParameters()"
    }

    /**
     * Keeps the password erasable while preserving the protocol's single nested `attributes`
     * object. The adapter serializes the character array as a JSON string only at the HTTP boundary.
     */
    @JsonAdapter(NativeAuthV2SubmitAttributesJsonAdapter::class)
    data class NativeAuthV2SubmitAttributes(
        val values: Map<String, String>,
        val password: CharArray?
    ) {
        val names: Set<String>
            get() = if (password == null || password.isEmpty()) values.keys else values.keys + PASSWORD
    }

    class NativeAuthV2SubmitAttributesJsonAdapter : TypeAdapter<NativeAuthV2SubmitAttributes>() {
        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: NativeAuthV2SubmitAttributes?) {
            if (value == null) {
                out.nullValue()
                return
            }

            out.beginObject()
            value.values.forEach { (name, attributeValue) ->
                out.name(name).value(attributeValue)
            }
            value.password?.takeUnless { it.isEmpty() }?.let { password ->
                out.name(PASSWORD)
                CharArrayJsonAdapter().write(out, password)
            }
            out.endObject()
        }

        @Throws(IOException::class)
        override fun read(input: JsonReader): NativeAuthV2SubmitAttributes {
            throw UnsupportedOperationException("NativeAuthV2SubmitAttributes is write-only.")
        }
    }
}
