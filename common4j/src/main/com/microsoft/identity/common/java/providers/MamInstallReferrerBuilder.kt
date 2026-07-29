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

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.CommonURIBuilder
import java.net.URISyntaxException

/**
 * Tags the Company Portal install launch with the calling app package as the Play install referrer,
 * for the MAM Conditional Access onboarding flow (Feature AB#3676213, Phase 1).
 *
 * **Why.** When Conditional Access blocks an interactive request until Company Portal is installed,
 * the user is sent to the Play Store. Without a referrer, Company Portal opens its own sign-in UX
 * on first launch - which is both an extra sign-in and a way for the user to accidentally enrol the
 * device into MDM. Company Portal already knows how to skip that UX and redirect back to the app
 * that caused the install when the Play install referrer names it, so this is the whole of the
 * client-side change: append `referrer=<callingAppPackage>`.
 *
 * **Scope.** Only the MAM-CA install path is tagged. The same broker-install redirect also drives
 * ordinary device-registration installs, which must keep their existing behavior, so the decoration
 * is gated on [MamCaRedirect.isMamCaInstall] as well as on
 * [CommonFlight.ENABLE_MAM_CA_INSTALL_REFERRER].
 *
 * **Safety.** Every entry point returns the original `app_link` unchanged if anything is missing or
 * unparseable, so a broker install is never broken by referrer decoration.
 */
object MamInstallReferrerBuilder {

    private val TAG = MamInstallReferrerBuilder::class.java.simpleName

    /** The Play install referrer query-parameter name. */
    const val REFERRER_QUERY_PARAM = "referrer"

    /**
     * The single place the MAM-CA install-referrer decision is made: should this Company Portal
     * install link be tagged with the calling app as the Play install referrer?
     *
     * Every broker-install launch site (the embedded WebView and the custom-tab / browser
     * authorization fragments) funnels through here, so the gate is evaluated once rather than being
     * copy-pasted per call site. Returns [appLink] unchanged when the flight is off, when the
     * redirect is not a MAM-CA install, or when [originPkg] is missing.
     *
     * Nothing at all happens - not even a log line - while the flight is off, so turning it off is a
     * complete kill switch. With the flight on, the redirect's parameter *names* are logged for
     * every broker install, marked or not: whether the server has started marking MAM-CA installs is
     * exactly the thing being rolled out, and it cannot be read off a log that only fires once the
     * marker is already there.
     *
     * @param appLink            the server-provided Play Store install link.
     * @param originPkg          the calling app package name (typically `Context#getPackageName()`).
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return the decorated link when MAM-CA referrer tagging applies, otherwise the original
     * [appLink].
     */
    @JvmStatic
    fun decorateAppLinkForMamCaInstall(
        appLink: String?,
        originPkg: String?,
        redirectParameters: Map<String, String>?
    ): String? {
        val methodTag = "$TAG:decorateAppLinkForMamCaInstall"

        if (!CommonFlightsManager.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_MAM_CA_INSTALL_REFERRER)
        ) {
            return appLink
        }

        // Names only - the redirect carries the user's UPN, so the URL itself is never logged.
        MamCaRedirect.logRedirectParameterNames(methodTag, redirectParameters)

        if (originPkg.isNullOrEmpty() || !MamCaRedirect.isMamCaInstall(redirectParameters)) {
            return appLink
        }

        return decorateAppLinkWithOriginReferrer(appLink, originPkg)
    }

    /**
     * Appends a single `referrer=<originPkg>` to the server-provided `app_link`, the form Company
     * Portal already recognises to skip its sign-in UX and redirect back to the calling app.
     *
     * A referrer the server already put on the link is left alone. The server names the calling app
     * directly, whereas [originPkg] can only ever be the package of the process that happens to be
     * hosting the sign-in UI - which is the calling app in the no-broker MAM onboarding case this
     * targets, but is the broker when one is installed and hosting the flow on the app's behalf.
     * Where the two disagree the server is right, so this decoration is a fallback for links that
     * arrive without a referrer, not an override.
     *
     * Ungated - callers decide whether decoration applies; use [decorateAppLinkForMamCaInstall] for
     * the gated entry point. Safe by design: if the `app_link` or [originPkg] is null/blank, or the
     * link cannot be parsed, the original `app_link` is returned unchanged.
     *
     * @param appLink   the server-provided Play Store install link.
     * @param originPkg the package hosting the sign-in UI.
     * @return the decorated link, or the original `app_link` if decoration is not possible.
     */
    @JvmStatic
    fun decorateAppLinkWithOriginReferrer(appLink: String?, originPkg: String?): String? {
        val methodTag = "$TAG:decorateAppLinkWithOriginReferrer"

        if (appLink.isNullOrEmpty() || originPkg.isNullOrEmpty()) {
            return appLink
        }
        return try {
            val builder = CommonURIBuilder(appLink)
            if (builder.queryParams.any { REFERRER_QUERY_PARAM.equals(it.name, ignoreCase = true) }) {
                Logger.info(
                    methodTag,
                    "The install link already names an install referrer; leaving it as it is."
                )
                appLink
            } else {
                val decorated = builder
                    .addParameterIfAbsent(REFERRER_QUERY_PARAM, originPkg)
                    .build()
                    .toString()
                Logger.info(
                    methodTag,
                    "Tagged the Company Portal install launch with the calling app as the install referrer."
                )
                decorated
            }
        } catch (e: URISyntaxException) {
            Logger.warn(
                methodTag,
                "Could not parse app_link to append the install referrer; launching it unchanged."
            )
            appLink
        }
    }
}
