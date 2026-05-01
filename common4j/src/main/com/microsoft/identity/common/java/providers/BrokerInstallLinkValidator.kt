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
import org.apache.hc.core5.http.NameValuePair
import java.net.URISyntaxException

/**
 * Strict allowlist validator for the `app_link` query-parameter value carried on
 * a broker-installation redirect URI (`msauth://...?app_link=<url>`).
 *
 * The classifier in [RawAuthorizationResult.getResultCodeFromFinalRedirectUri] uses this to
 * decide whether to return [RawAuthorizationResult.ResultCode.BROKER_INSTALLATION_TRIGGERED]
 * (which downstream Android sinks turn into `startActivity(ACTION_VIEW, app_link)`).
 *
 * The Microsoft identity service (eSTS) only ever emits one of three concrete
 * `app_link` values today:
 *  1. `https://play.google.com/store/apps/details?id=com.azure.authenticator`
 *  2. `https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal`
 *  3. `https://go.microsoft.com/fwlink/?linkid=2134649` (China Company Portal)
 * Each may carry an optional `referrer` parameter set by the server.
 */
object BrokerInstallLinkValidator {

    private const val SCHEME_HTTPS = "https"
    private const val HOST_PLAY = "play.google.com"
    private const val HOST_FWLINK = "go.microsoft.com"
    private const val PATH_PLAY = "/store/apps/details"
    private const val PATH_FWLINK = "/fwlink"
    private const val PATH_FWLINK_TRAILING = "/fwlink/"

    private const val PARAM_ID = "id"
    private const val PARAM_LINKID = "linkid"
    private const val PARAM_REFERRER = "referrer"

    private val ALLOWED_PACKAGE_IDS = setOf(
        "com.azure.authenticator",
        "com.microsoft.windowsintune.companyportal"
    )

    private val ALLOWED_FWLINK_IDS = setOf("2134649")

    /**
     * @return `true` iff [url] decodes to one of the allowlisted broker-install
     * destinations defined above; `false` for any other input (including null,
     * blank, malformed, or attacker-controlled values).
     */
    @JvmStatic
    fun isSafeBrokerInstallLink(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        val builder: CommonURIBuilder = try {
            CommonURIBuilder(url)
        } catch (e: URISyntaxException) {
            return false
        }

        // Scheme must be exactly https (case-insensitive).
        if (!SCHEME_HTTPS.equals(builder.scheme, ignoreCase = true)) return false

        // Reject embedded credentials, fragments, and non-default ports.
        if (builder.userInfo != null) return false
        if (builder.fragment != null) return false
        if (builder.port != -1) return false

        val host = builder.host?.lowercase() ?: return false
        val path = builder.path ?: return false

        // Use getQueryParams() from CommonURIBuilder to parse query parameters.
        // toUniqueParamMap returns null if any key appears more than once, defending
        // against parameter-smuggling attacks such as ?id=safe&id=evil.
        val params = toUniqueParamMap(builder.queryParams) ?: return false

        return when (host) {
            HOST_PLAY -> isValidPlayLink(path, params)
            HOST_FWLINK -> isValidFwlink(path, params)
            else -> false
        }
    }

    private fun isValidPlayLink(path: String, params: Map<String, String>): Boolean {
        if (path != PATH_PLAY) return false
        val id = params[PARAM_ID] ?: return false
        if (id !in ALLOWED_PACKAGE_IDS) return false
        return hasOnlyAllowedExtras(params, PARAM_ID)
    }

    private fun isValidFwlink(path: String, params: Map<String, String>): Boolean {
        if (path != PATH_FWLINK && path != PATH_FWLINK_TRAILING) return false
        val linkId = params[PARAM_LINKID] ?: return false
        if (linkId !in ALLOWED_FWLINK_IDS) return false
        return hasOnlyAllowedExtras(params, PARAM_LINKID)
    }

    private fun hasOnlyAllowedExtras(params: Map<String, String>, requiredKey: String): Boolean {
        for (key in params.keys) {
            if (key == requiredKey) continue
            if (key == PARAM_REFERRER) continue
            return false
        }
        return true
    }

    /**
     * Converts a [NameValuePair] list (from [CommonURIBuilder.getQueryParams]) into a map.
     *
     * Returns `null` if any key appears more than once — this defends against
     * parameter-smuggling attacks such as `?id=safe&id=evil` where a permissive
     * parser might pick the wrong value.
     */
    private fun toUniqueParamMap(pairs: List<NameValuePair>): Map<String, String>? {
        val out = LinkedHashMap<String, String>()
        for (pair in pairs) {
            if (out.containsKey(pair.name)) return null
            out[pair.name] = pair.value ?: ""
        }
        return out
    }
}
