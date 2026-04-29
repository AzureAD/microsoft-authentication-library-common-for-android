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

import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/**
 * Strict allowlist validator for the `app_link` query-parameter value carried on
 * a broker-installation redirect URI (`msauth://...?app_link=<url>`).
 *
 * The classifier in [RawAuthorizationResult.fromRedirectUri] uses this to decide
 * whether to return [RawAuthorizationResult.ResultCode.BROKER_INSTALLATION_TRIGGERED]
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

        val uri: URI = try {
            URI(url)
        } catch (e: URISyntaxException) {
            return false
        }

        // Scheme must be exactly https (case-insensitive).
        if (!SCHEME_HTTPS.equals(uri.scheme, ignoreCase = true)) return false

        // Reject embedded credentials, fragments, and non-default ports.
        if (uri.rawUserInfo != null) return false
        if (uri.rawFragment != null) return false
        if (uri.port != -1) return false

        val host = uri.host?.lowercase() ?: return false
        val path = uri.rawPath ?: return false
        val params = parseQuery(uri.rawQuery) ?: return false

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
     * Parse a URI query string into a key/value map.
     *
     * Returns `null` if any key appears more than once — this defends against
     * parameter-smuggling attacks such as `?id=safe&id=evil` where a permissive
     * parser might pick the wrong value.
     */
    private fun parseQuery(rawQuery: String?): Map<String, String>? {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in rawQuery.split('&')) {
            if (pair.isEmpty()) return null
            val eq = pair.indexOf('=')
            val rawKey: String
            val rawValue: String
            if (eq < 0) {
                rawKey = pair
                rawValue = ""
            } else {
                rawKey = pair.substring(0, eq)
                rawValue = pair.substring(eq + 1)
            }
            if (rawKey.isEmpty()) return null
            val key = try {
                URLDecoder.decode(rawKey, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                return null
            } catch (e: IllegalArgumentException) {
                return null
            }
            val value = try {
                URLDecoder.decode(rawValue, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                return null
            } catch (e: IllegalArgumentException) {
                return null
            }
            if (out.containsKey(key)) return null
            out[key] = value
        }
        return out
    }
}
