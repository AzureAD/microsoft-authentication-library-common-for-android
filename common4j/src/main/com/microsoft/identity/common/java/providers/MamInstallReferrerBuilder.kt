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
 * is gated on [MamCaRedirect.isMamCaInstall] as well as on the host's opt-in.
 *
 * **Who turns it on.** The `enabled` flag comes from the host SDK (OneAuth/MSAL), not from a flight
 * read here: common4j flights are only initialized in the broker process, so a flight read on this
 * path would always be its compile-time default. Defaults to `false`, so hosts that do not pass it
 * are unchanged.
 *
 * The marker is the whole of the scope, deliberately. The server sets `intuneAppProtection=1` only
 * on MAM-CA flows, so once it is present the redirect is known to be one, and the referrer is
 * always appended. In particular this does **not** additionally check which application the
 * `app_link` installs: all of the destinations a broker-install redirect may carry (see
 * `BrokerInstallLinkValidator`) are legitimate here - including the China Company Portal
 * `go.microsoft.com` fwlink, which the server itself may already carry a `referrer` on. Narrowing
 * on the install target would drop the referrer on real MAM-CA installs; see
 * `gated_markedInstall_isDecoratedWhateverTheInstallTargetIs`.
 *
 * **Safety.** Every entry point returns the original `app_link` unchanged if anything is missing or
 * unparseable, so a broker install is never broken by referrer decoration.
 */
object MamInstallReferrerBuilder {

    private val TAG = MamInstallReferrerBuilder::class.java.simpleName

    /** The Play install referrer query-parameter name. */
    const val REFERRER_QUERY_PARAM = "referrer"

    /**
     * Why a Company Portal install link was, or was not, tagged with an install referrer.
     *
     * Six of the seven outcomes return the caller's `app_link` unchanged, so the returned link alone
     * cannot say *why* decoration did not happen - "the host did not opt in" and "the server already
     * named a referrer" are indistinguishable from it. This enum is what makes those cases separable,
     * in tests and when reading a call site.
     *
     * Deliberately carries no telemetry tag. Rollout is read off the log lines below - every outcome
     * that is actually evaluated names itself in one - not off the onboarding blob: `ux_flow_used`
     * records which UX-variant cohort a user was in, not per-attempt result codes, so these values do
     * not belong there.
     */
    enum class Outcome {

        /**
         * The host did not opt in, so nothing was evaluated. While off this feature is meant to be
         * indistinguishable from its absence, log lines included.
         */
        NOT_ENABLED,

        /** The redirect carried no MAM-CA marker, so it is an ordinary broker install. */
        NOT_MAM_CA,

        /** A MAM-CA install, but the host package name was unavailable to name as the referrer. */
        NO_ORIGIN_PKG,

        /** A MAM-CA install, but the redirect carried no `app_link` to decorate. */
        NO_APP_LINK,

        /** A MAM-CA install whose `app_link` already named a referrer; the server's wins. */
        SERVER_REFERRER,

        /** A MAM-CA install whose `app_link` could not be parsed; launched unchanged. */
        LINK_UNPARSEABLE,

        /** A MAM-CA install that was tagged with the host package as the install referrer. */
        DECORATED
    }

    /**
     * The link to launch, and why it did or did not get an install referrer.
     *
     * @property appLink the link to launch - decorated only when [outcome] is [Outcome.DECORATED],
     * and otherwise the caller's original `app_link`, unchanged.
     * @property outcome why decoration did or did not happen.
     */
    data class Decoration(val appLink: String?, val outcome: Outcome)

    /**
     * The single place the MAM-CA install-referrer decision is made: should this Company Portal
     * install link be tagged with the calling app as the Play install referrer?
     *
     * Every broker-install launch site (the embedded WebView and the custom-tab / browser
     * authorization fragments) funnels through here, so the gate is evaluated once rather than being
     * copy-pasted per call site. Returns [appLink] unchanged when [enabled] is false, when the
     * redirect is not a MAM-CA install, or when [originPkg] is missing.
     *
     * Nothing at all happens - not even a log line - while [enabled] is false, so turning it off is a
     * complete kill switch. Once on, the redirect's parameter *names* are logged for every broker
     * install, marked or not: whether the server has started marking MAM-CA installs is exactly the
     * thing being rolled out, and it cannot be read off a log that only fires once the marker is
     * already there.
     *
     * Use [decorateAppLinkForMamCaInstallWithOutcome] where the caller needs to know *why*.
     *
     * @param enabled            whether the host opted in to MAM-CA install-referrer tagging.
     * @param appLink            the server-provided Play Store install link.
     * @param originPkg          the calling app package name (typically `Context#getPackageName()`).
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return the decorated link when MAM-CA referrer tagging applies, otherwise the original
     * [appLink].
     */
    @JvmStatic
    fun decorateAppLinkForMamCaInstall(
        enabled: Boolean,
        appLink: String?,
        originPkg: String?,
        redirectParameters: Map<String, String>?
    ): String? =
        decorateAppLinkForMamCaInstallWithOutcome(enabled, appLink, originPkg, redirectParameters).appLink

    /**
     * [decorateAppLinkForMamCaInstall], additionally returning *why* the link was or was not
     * decorated. Six of the seven outcomes launch the link unchanged, so the returned link alone
     * cannot distinguish them.
     *
     * The MAM-CA marker is checked before [originPkg] so that a missing package name surfaces as
     * a bail-out on a *marked* redirect rather than hiding whether the marker was there at all -
     * the marker is the thing being rolled out, so it is the more valuable of the two signals.
     * Both cases return the link unchanged, so the ordering affects only the reported outcome.
     *
     * @param enabled            whether the host opted in to MAM-CA install-referrer tagging.
     * @param appLink            the server-provided Play Store install link.
     * @param originPkg          the calling app package name (typically `Context#getPackageName()`).
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return the link to launch, and the outcome that produced it.
     */
    @JvmStatic
    fun decorateAppLinkForMamCaInstallWithOutcome(
        enabled: Boolean,
        appLink: String?,
        originPkg: String?,
        redirectParameters: Map<String, String>?
    ): Decoration {
        val methodTag = "$TAG:decorateAppLinkForMamCaInstall"

        if (!enabled) {
            return Decoration(appLink, Outcome.NOT_ENABLED)
        }

        // Names only - the redirect carries the user's UPN, so the URL itself is never logged.
        MamCaRedirect.logRedirectParameterNames(methodTag, redirectParameters)

        if (!MamCaRedirect.isMamCaInstall(redirectParameters)) {
            Logger.info(
                methodTag,
                "The redirect carries no MAM-CA marker, so this is an ordinary broker install; " +
                    "leaving the install link as it is. [outcome=${Outcome.NOT_MAM_CA}]"
            )
            return Decoration(appLink, Outcome.NOT_MAM_CA)
        }
        if (originPkg.isNullOrEmpty()) {
            Logger.info(
                methodTag,
                "A MAM-CA install, but the calling app package is unavailable to name as the " +
                    "install referrer; leaving the install link as it is. " +
                    "[outcome=${Outcome.NO_ORIGIN_PKG}]"
            )
            return Decoration(appLink, Outcome.NO_ORIGIN_PKG)
        }

        return decorateWithOutcome(appLink, originPkg)
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
        if (originPkg.isNullOrEmpty()) {
            return appLink
        }
        return decorateWithOutcome(appLink, originPkg).appLink
    }

    /**
     * [decorateAppLinkWithOriginReferrer] with the reason attached. Assumes [originPkg] is present;
     * the gated entry point has already established that.
     */
    private fun decorateWithOutcome(appLink: String?, originPkg: String): Decoration {
        val methodTag = "$TAG:decorateAppLinkWithOriginReferrer"

        if (appLink.isNullOrEmpty()) {
            Logger.info(
                methodTag,
                "The redirect carried no app_link to tag with an install referrer. " +
                    "[outcome=${Outcome.NO_APP_LINK}]"
            )
            return Decoration(appLink, Outcome.NO_APP_LINK)
        }
        return try {
            val builder = CommonURIBuilder(appLink)
            // This check must stay ahead of addParameterIfAbsent, but not for case-handling reasons:
            // CommonURIBuilder.containsParam already compares with equalsIgnoreCase, so both agree on
            // a mixed-case `Referrer=`. What the explicit branch earns is the two things
            // addParameterIfAbsent cannot express - returning the caller's original string rather
            // than a re-serialised build(), and logging "left it alone" rather than "tagged it".
            if (builder.queryParams.any { REFERRER_QUERY_PARAM.equals(it.name, ignoreCase = true) }) {
                Logger.info(
                    methodTag,
                    "The install link already names an install referrer; leaving it as it is. " +
                        "[outcome=${Outcome.SERVER_REFERRER}]"
                )
                Decoration(appLink, Outcome.SERVER_REFERRER)
            } else {
                val decorated = builder
                    .addParameterIfAbsent(REFERRER_QUERY_PARAM, originPkg)
                    .build()
                    .toString()
                Logger.info(
                    methodTag,
                    "Tagged the Company Portal install launch with the calling app as the install " +
                        "referrer. [outcome=${Outcome.DECORATED}]"
                )
                Decoration(decorated, Outcome.DECORATED)
            }
        } catch (e: URISyntaxException) {
            Logger.warn(
                methodTag,
                "Could not parse app_link to append the install referrer; launching it unchanged. " +
                    "[outcome=${Outcome.LINK_UNPARSEABLE}]"
            )
            Decoration(appLink, Outcome.LINK_UNPARSEABLE)
        }
    }
}
