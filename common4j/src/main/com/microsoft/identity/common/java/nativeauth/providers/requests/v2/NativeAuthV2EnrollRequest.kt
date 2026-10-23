// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
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
