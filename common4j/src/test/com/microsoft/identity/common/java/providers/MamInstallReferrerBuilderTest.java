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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.identity.common.java.util.CommonURIBuilder;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.NameValuePair;

/**
 * Unit tests for {@link MamInstallReferrerBuilder} (PBI-2). These graduate the earlier proof-of-concept
 * into production coverage: they build the packed referrer, prove it keeps the Company Portal Play link
 * on the real {@link BrokerInstallLinkValidator} allow-list, prove the encode/decode round-trip recovers
 * a base64 redirect URI byte-for-byte, and prove the decoration is null-safe (never breaks the existing
 * install launch).
 */
public class MamInstallReferrerBuilderTest {

    private static final String CP_ID = "com.microsoft.windowsintune.companyportal";
    private static final String CP_APP_LINK = "https://play.google.com/store/apps/details?id=" + CP_ID;
    private static final String ORIGIN_PKG = "com.microsoft.office.outlook";
    // Realistic msauth redirect: base64 SHA-1 signature contains the tricky '+', '/', '=' characters.
    private static final String REDIRECT_URI = "msauth://com.microsoft.office.outlook/GC+pJ8k9dItg3F1lZ7q2rY0aBcD=";
    private static final String CID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    // region buildReferrerValue

    @Test
    public void buildReferrerValue_startsWithSrcMamCa_andIsBoundedLength() {
        final String referrer = MamInstallReferrerBuilder.buildReferrerValue(ORIGIN_PKG, REDIRECT_URI, CID);
        assertTrue("src=mamca must be the discriminator", referrer.startsWith("src=mamca"));
        assertTrue(referrer.contains("originPkg="));
        assertTrue(referrer.contains("redirectUri="));
        assertTrue(referrer.contains("cid=" + CID));
        assertTrue("referrer must be comfortably under Play's practical limit",
                referrer.length() < 1024);
    }

    // endregion

    // region decorateAppLinkWithReferrer

    @Test
    public void decorate_keepsCompanyPortalLinkOnAllowlist() {
        final String decorated = MamInstallReferrerBuilder.decorateAppLinkWithReferrer(
                CP_APP_LINK, ORIGIN_PKG, REDIRECT_URI, CID);

        assertTrue("decorated app_link must remain allow-listed:\n" + decorated,
                BrokerInstallLinkValidator.isSafeBrokerInstallLink(decorated));
    }

    @Test
    public void decorate_addsExactlyOneReferrerParam() throws Exception {
        final String decorated = MamInstallReferrerBuilder.decorateAppLinkWithReferrer(
                CP_APP_LINK, ORIGIN_PKG, REDIRECT_URI, CID);

        int referrerCount = 0;
        final List<NameValuePair> params = new CommonURIBuilder(decorated).getQueryParams();
        for (final NameValuePair p : params) {
            if (MamInstallReferrerBuilder.REFERRER_QUERY_PARAM.equals(p.getName())) {
                referrerCount++;
            }
        }
        assertEquals("exactly one referrer parameter", 1, referrerCount);
    }

    @Test
    public void decorate_returnsOriginalUnchanged_whenAnyInputMissing() {
        assertEquals(CP_APP_LINK,
                MamInstallReferrerBuilder.decorateAppLinkWithReferrer(CP_APP_LINK, null, REDIRECT_URI, CID));
        assertEquals(CP_APP_LINK,
                MamInstallReferrerBuilder.decorateAppLinkWithReferrer(CP_APP_LINK, ORIGIN_PKG, "", CID));
        assertEquals(CP_APP_LINK,
                MamInstallReferrerBuilder.decorateAppLinkWithReferrer(CP_APP_LINK, ORIGIN_PKG, REDIRECT_URI, null));
        assertNull(MamInstallReferrerBuilder.decorateAppLinkWithReferrer(null, ORIGIN_PKG, REDIRECT_URI, CID));
    }

    // endregion

    // region round-trip (simulated Play delivery -> Company Portal parse)

    @Test
    public void referrer_roundTrips_recoveringBase64RedirectByteForByte() throws Exception {
        final String decorated = MamInstallReferrerBuilder.decorateAppLinkWithReferrer(
                CP_APP_LINK, ORIGIN_PKG, REDIRECT_URI, CID);

        // The value Google Play hands to the installed app is the referrer param value, decoded once.
        String deliveredReferrer = null;
        for (final NameValuePair p : new CommonURIBuilder(decorated).getQueryParams()) {
            if (MamInstallReferrerBuilder.REFERRER_QUERY_PARAM.equals(p.getName())) {
                deliveredReferrer = p.getValue();
            }
        }

        final Map<String, String> parsed = MamInstallReferrerBuilder.parseReferrer(deliveredReferrer);
        assertEquals(MamInstallReferrerBuilder.SRC_MAM_CA, parsed.get(MamInstallReferrerBuilder.KEY_SRC));
        assertEquals(ORIGIN_PKG, parsed.get(MamInstallReferrerBuilder.KEY_ORIGIN_PKG));
        assertEquals(CID, parsed.get(MamInstallReferrerBuilder.KEY_CID));
        assertEquals("base64 redirect (with + / =) must survive the round-trip",
                REDIRECT_URI, parsed.get(MamInstallReferrerBuilder.KEY_REDIRECT_URI));
    }

    @Test
    public void parseReferrer_emptyOrNull_returnsEmptyMap() {
        assertTrue(MamInstallReferrerBuilder.parseReferrer(null).isEmpty());
        assertTrue(MamInstallReferrerBuilder.parseReferrer("").isEmpty());
    }

    // endregion

    // region market:// fallback

    @Test
    public void marketFallback_carriesReferrer_targetsBroker_andIsNotHttps() {
        final String market = MamInstallReferrerBuilder.buildMarketFallbackUri(CP_ID, ORIGIN_PKG, REDIRECT_URI, CID);

        assertTrue(market.startsWith("market://details?id=" + CP_ID));
        assertTrue(market.contains("referrer="));
        // Not https, so it must be launched directly and never fed to the https-only allow-list validator.
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink(market));
    }

    @Test
    public void marketFallback_nullPackageId_returnsNull() {
        assertNull(MamInstallReferrerBuilder.buildMarketFallbackUri(null, ORIGIN_PKG, REDIRECT_URI, CID));
    }

    // endregion
}
