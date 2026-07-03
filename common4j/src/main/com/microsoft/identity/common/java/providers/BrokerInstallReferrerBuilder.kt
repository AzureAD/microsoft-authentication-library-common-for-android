// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.providers

import com.microsoft.identity.common.java.util.CommonURIBuilder
import java.net.URISyntaxException

/**
 * Adds the broker-install-resume **pointer** to a Play Store install link. Only the correlation id
 * (and origin package) ride the install referrer — never the request payload or PII such as the
 * login hint — so nothing sensitive transits the Play Store. The freshly installed broker reads the
 * referrer to look up the request locally and resume it. The link must already be allowlisted by
 * [BrokerInstallLinkValidator]; this builder does not alter the destination.
 */
object BrokerInstallReferrerBuilder {

    private const val PARAM_REFERRER = "referrer"
    private const val POINTER_CORRELATION_ID = "resumeCid"
    private const val POINTER_PACKAGE = "originPkg"
    private const val POINTER_REDIRECT_URI = "redirectUri"

    /**
     * Appends an encoded resume pointer to [installUrl]'s `referrer` parameter.
     *
     * @param installUrl Allowlisted Play/fwlink install URL.
     * @param correlationId Single-use resume key.
     * @param originPackage Package that triggered the install; used for verification on resume.
     * @param redirectUri The app's msauth redirect uri; Company Portal uses it to redirect the user
     *   straight back to the calling app after install (already public — never PII).
     * @return URL with the pointer set, or the original [installUrl] if it cannot be parsed.
     */
    @JvmStatic
    @JvmOverloads
    fun withResumePointer(
        installUrl: String,
        correlationId: String,
        originPackage: String,
        redirectUri: String? = null
    ): String {
        val builder: CommonURIBuilder = try {
            CommonURIBuilder(installUrl)
        } catch (e: URISyntaxException) {
            return installUrl
        }
        val pointer = buildString {
            append("$POINTER_CORRELATION_ID=$correlationId;$POINTER_PACKAGE=$originPackage")
            if (!redirectUri.isNullOrBlank()) {
                append(";$POINTER_REDIRECT_URI=$redirectUri")
            }
        }
        builder.setParameter(PARAM_REFERRER, pointer)
        return builder.toString()
    }

    /** Parses a referrer pointer into [correlationId, originPackage], or null if not a resume pointer. */
    @JvmStatic
    fun parseResumePointer(referrer: String?): Pair<String, String>? {
        if (referrer.isNullOrBlank()) return null
        val parts = referrer.split(";").mapNotNull {
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()
        val cid = parts[POINTER_CORRELATION_ID] ?: return null
        val pkg = parts[POINTER_PACKAGE] ?: return null
        return cid to pkg
    }
}
