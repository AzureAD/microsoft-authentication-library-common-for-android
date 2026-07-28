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

package com.microsoft.identity.common.java.providers;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.URISyntaxException;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Tags the Company Portal install launch with the calling app package as the Play install referrer,
 * for the MAM Conditional Access onboarding flow (Feature AB#3676213, Phase 1).
 * <p>
 * <b>Why.</b> When Conditional Access blocks an interactive request until Company Portal is
 * installed, the user is sent to the Play Store. Without a referrer, Company Portal opens its own
 * sign-in UX on first launch - which is both an extra sign-in and a way for the user to accidentally
 * enrol the device into MDM. Company Portal already knows how to skip that UX and redirect back to
 * the app that caused the install when the Play install referrer names it, so this is the whole of
 * the client-side change: append {@code referrer=<callingAppPackage>}.
 * <p>
 * <b>Scope.</b> Only the MAM-CA install path is tagged. The same {@code msauth://wpj} redirect also
 * drives ordinary device-registration installs, which must keep their existing behavior, so the
 * decoration is gated on {@link MamCaRedirect#isMamCaInstall(Map)} as well as on
 * {@link CommonFlight#ENABLE_MAM_CA_INSTALL_REFERRER}.
 * <p>
 * <b>Safety.</b> Every entry point returns the original {@code app_link} unchanged if anything is
 * missing or unparseable, so a broker install is never broken by referrer decoration.
 */
public final class MamInstallReferrerBuilder {

    private static final String TAG = MamInstallReferrerBuilder.class.getSimpleName();

    /** The Play install referrer query-parameter name. */
    public static final String REFERRER_QUERY_PARAM = "referrer";

    private MamInstallReferrerBuilder() {
        // Utility class.
    }

    /**
     * The single place the MAM-CA install-referrer decision is made: should this Company Portal
     * install link be tagged with the calling app as the Play install referrer?
     * <p>
     * Every broker-install launch site (the embedded WebView and the custom-tab / browser
     * authorization fragments) funnels through here, so the gate is evaluated once rather than being
     * copy-pasted per call site. Returns {@code appLink} unchanged when the flight is off, when the
     * redirect is not a MAM-CA install, or when {@code originPkg} is missing.
     *
     * @param appLink            the server-provided Play Store install link.
     * @param originPkg          the calling app package name (typically {@code Context#getPackageName()}).
     * @param redirectParameters query parameters of the {@code msauth://wpj} broker-install redirect.
     * @return the decorated link when MAM-CA referrer tagging applies, otherwise the original {@code appLink}.
     */
    public static String decorateAppLinkForMamCaInstall(final String appLink,
                                                        final String originPkg,
                                                        @Nullable final Map<String, String> redirectParameters) {
        final String methodTag = TAG + ":decorateAppLinkForMamCaInstall";

        if (StringUtil.isNullOrEmpty(originPkg)
                || !CommonFlightsManager.INSTANCE.getFlightsProvider()
                        .isFlightEnabled(CommonFlight.ENABLE_MAM_CA_INSTALL_REFERRER)
                || !MamCaRedirect.isMamCaInstall(redirectParameters)) {
            return appLink;
        }

        final String decorated = decorateAppLinkWithOriginReferrer(appLink, originPkg);
        Logger.info(methodTag,
                "Tagged the Company Portal install launch with the calling app as the install referrer.");
        return decorated;
    }

    /**
     * Appends a single {@code referrer=<originPkg>} to the server-provided {@code app_link}, the form
     * Company Portal already recognises to skip its sign-in UX and redirect back to the calling app.
     * <p>
     * Ungated - callers decide whether decoration applies; use
     * {@link #decorateAppLinkForMamCaInstall(String, String, Map)} for the gated entry point. Safe by
     * design: if the {@code app_link} or {@code originPkg} is null/blank, or the link cannot be
     * parsed, the original {@code app_link} is returned unchanged.
     *
     * @param appLink   the server-provided Play Store install link.
     * @param originPkg the calling app package name.
     * @return the decorated link, or the original {@code app_link} if decoration is not possible.
     */
    public static String decorateAppLinkWithOriginReferrer(final String appLink, final String originPkg) {
        if (StringUtil.isNullOrEmpty(appLink) || StringUtil.isNullOrEmpty(originPkg)) {
            return appLink;
        }
        try {
            // setParameter (not addParameter) so exactly one referrer results, whatever the link
            // already carried.
            return new CommonURIBuilder(appLink)
                    .setParameter(REFERRER_QUERY_PARAM, originPkg)
                    .build()
                    .toString();
        } catch (final URISyntaxException e) {
            Logger.warn(TAG + ":decorateAppLinkWithOriginReferrer",
                    "Could not parse app_link to append the install referrer; launching it unchanged.");
            return appLink;
        }
    }
}
