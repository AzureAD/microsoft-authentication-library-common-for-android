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

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the Play Store install referrer for the MAM broker-install request-resume flow, and decorates
 * the broker install link with it (PBI-2, Feature AB#3676213).
 * <p>
 * The referrer carries routing data only — {@code src=mamca}, the calling app package, the app's
 * redirect URI, and the parked request correlation id — so Company Portal can, on first launch, skip
 * its sign-in UX and redirect back to {@code <redirectUri>?mam_resume=<cid>}. It carries no UPN or
 * secrets (the UPN is out-of-band; see the resume engine).
 * <p>
 * Wire contract (frozen): {@code src=mamca&originPkg=<pkg>&redirectUri=<uri>&cid=<cid>}, URL-encoded,
 * {@code &}-delimited {@code key=value}; parsed with {@code URLDecoder.decode} then
 * {@code Uri.parse("?"+s).getQueryParameter(k)} (the OneAuth {@code AccountTransfer} convention).
 * <p>
 * Two launch forms are supported: the primary decorates the eSTS-provided {@code https://play.google.com}
 * {@code app_link} with a single {@code referrer} parameter (kept on the {@code BrokerInstallLinkValidator}
 * allow-list); the fallback builds a {@code market://details?id=<pkg>&referrer=<...>} URI. Both feed the
 * same Play Install Referrer API on the installed app.
 */
public final class MamInstallReferrerBuilder {

    private static final String TAG = MamInstallReferrerBuilder.class.getSimpleName();

    /** Referrer discriminator key. */
    public static final String KEY_SRC = "src";
    /** Calling app package name key. */
    public static final String KEY_ORIGIN_PKG = "originPkg";
    /** App redirect URI key (Company Portal appends {@code ?mam_resume=<cid>} to this). */
    public static final String KEY_REDIRECT_URI = "redirectUri";
    /** Parked request correlation id key. */
    public static final String KEY_CID = "cid";

    /** Locked discriminator value; Company Portal branches on {@code src == mamca}. */
    public static final String SRC_MAM_CA = "mamca";

    /** The Play install referrer query-parameter name. */
    public static final String REFERRER_QUERY_PARAM = "referrer";

    private static final String MARKET_DETAILS_PREFIX = "market://details?id=";
    private static final String UTF_8 = "UTF-8";

    private MamInstallReferrerBuilder() {
    }

    /**
     * Builds the packed referrer value {@code src=mamca&originPkg=..&redirectUri=..&cid=..} with each
     * value URL-encoded.
     *
     * @param originPkg   the calling app package name.
     * @param redirectUri the app's redirect URI.
     * @param cid         the parked request correlation id.
     * @return the packed, inner-encoded referrer value.
     */
    public static String buildReferrerValue(final String originPkg,
                                            final String redirectUri,
                                            final String cid) {
        return KEY_SRC + "=" + SRC_MAM_CA
                + "&" + KEY_ORIGIN_PKG + "=" + encode(originPkg)
                + "&" + KEY_REDIRECT_URI + "=" + encode(redirectUri)
                + "&" + KEY_CID + "=" + encode(cid);
    }

    /**
     * Primary launch form: appends the packed referrer as a single {@code referrer} parameter to the
     * eSTS-provided {@code app_link}. Uses {@link CommonURIBuilder} so the outer percent-encoding is
     * applied consistently and exactly one {@code referrer} parameter results.
     * <p>
     * Safe by design: if the {@code app_link} or any referrer input is null/blank, or the link cannot be
     * parsed, the original {@code app_link} is returned unchanged so the existing install flow is never
     * broken.
     *
     * @param appLink     the server-provided Play Store install link.
     * @param originPkg   the calling app package name.
     * @param redirectUri the app's redirect URI.
     * @param cid         the parked request correlation id.
     * @return the decorated link, or the original {@code app_link} if decoration is not possible.
     */
    public static String decorateAppLinkWithReferrer(final String appLink,
                                                     final String originPkg,
                                                     final String redirectUri,
                                                     final String cid) {
        if (StringUtil.isNullOrEmpty(appLink)
                || StringUtil.isNullOrEmpty(originPkg)
                || StringUtil.isNullOrEmpty(redirectUri)
                || StringUtil.isNullOrEmpty(cid)) {
            return appLink;
        }
        try {
            return new CommonURIBuilder(appLink)
                    .setParameter(REFERRER_QUERY_PARAM, buildReferrerValue(originPkg, redirectUri, cid))
                    .build()
                    .toString();
        } catch (final URISyntaxException e) {
            Logger.warn(TAG + ":decorateAppLinkWithReferrer",
                    "Could not parse app_link to append the install referrer; launching it unchanged.");
            return appLink;
        }
    }

    /**
     * Fallback launch form: builds {@code market://details?id=<playStoreId>&referrer=<encoded>} for use
     * when appending to the {@code app_link} does not reliably reach Play.
     *
     * @param playStoreId the broker Play Store package id (e.g. Company Portal).
     * @param originPkg   the calling app package name.
     * @param redirectUri the app's redirect URI.
     * @param cid         the parked request correlation id.
     * @return the {@code market://} install URI, or {@code null} if the package id is null/blank.
     */
    public static String buildMarketFallbackUri(final String playStoreId,
                                                final String originPkg,
                                                final String redirectUri,
                                                final String cid) {
        if (StringUtil.isNullOrEmpty(playStoreId)) {
            return null;
        }
        return MARKET_DETAILS_PREFIX + playStoreId
                + "&" + REFERRER_QUERY_PARAM + "=" + encode(buildReferrerValue(originPkg, redirectUri, cid));
    }

    /**
     * CP-compatible launch form (confirmed by the Company Portal team, Veena Soman, 2026-07-17): appends a
     * single bare {@code referrer=<originPkg>} to the eSTS-provided {@code app_link}, matching the
     * {@code &referrer=<originAppPackage>} pattern Company Portal already supports today. This is the form
     * used at the production launch site: it lets Company Portal identify — and redirect back to — the
     * calling app, which (combined with the in-process park registry + foreground-fallback resume) is
     * sufficient to resume without Company Portal having to round-trip the correlation id. The richer
     * {@link #decorateAppLinkWithReferrer} form is reserved for the automatic {@code mam_resume=<cid>} path
     * once Company Portal confirms it passes the full referrer value through to first launch.
     * <p>
     * Safe by design: if the {@code app_link} or {@code originPkg} is null/blank, or the link cannot be
     * parsed, the original {@code app_link} is returned unchanged so the existing install flow is never
     * broken.
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

    /**
     * Parses a packed referrer value back into its key/value pairs. This mirrors the parse Company
     * Portal performs on first launch (blueprint: OneAuth {@code AccountTransfer}).
     *
     * @param referrer the packed referrer value (as delivered by the Play Install Referrer API).
     * @return an ordered map of the decoded key/value pairs (never {@code null}).
     */
    public static Map<String, String> parseReferrer(final String referrer) {
        final Map<String, String> out = new LinkedHashMap<>();
        if (StringUtil.isNullOrEmpty(referrer)) {
            return out;
        }
        for (final String pair : referrer.split("&")) {
            final int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            out.put(pair.substring(0, idx), decode(pair.substring(idx + 1)));
        }
        return out;
    }

    private static String encode(final String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (final UnsupportedEncodingException e) {
            // UTF-8 is always supported; return the raw value defensively.
            return value;
        }
    }

    private static String decode(final String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLDecoder.decode(value, UTF_8);
        } catch (final UnsupportedEncodingException e) {
            return value;
        }
    }
}
