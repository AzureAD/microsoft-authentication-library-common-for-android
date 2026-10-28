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
import java.net.URL

/** Channel-specific enrollment body. Contact and token are never included in either log string. */
class NativeAuthV2EnrollRequest internal constructor(
    override var requestUrl: URL,
    override var headers: Map<String, String?>,
    override val parameters: EnrollmentParameters
) : NativeAuthRequest() {
    /** Contains only the continuation token and the selected channel's contact fields. */
    class EnrollmentParameters internal constructor(
        val continuationToken: String,
        val email: String? = null,
        val phoneNumber: String? = null,
        val channel: String? = null
    ) : NativeAuthRequestParameters() {
        override fun toString(): String = "EnrollmentParameters(<redacted>)"
        override fun toUnsanitizedString(): String = toString()
    }

    override fun toString(): String = "NativeAuthV2EnrollRequest(<redacted>)"
    override fun toUnsanitizedString(): String = toString()
}
