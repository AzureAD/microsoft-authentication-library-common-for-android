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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.util.CommonURIBuilder;

import org.apache.hc.core5.http.NameValuePair;
import org.junit.After;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link MamInstallReferrerBuilder} - the MAM Conditional Access Phase 1 install-referrer
 * decoration.
 */
public class MamInstallReferrerBuilderTest {

    private static final String CP_ID = "com.microsoft.windowsintune.companyportal";
    private static final String CP_APP_LINK = "https://play.google.com/store/apps/details?id=" + CP_ID;
    private static final String ORIGIN_PKG = "com.microsoft.office.outlook";

    // region decorateAppLinkWithOriginReferrer (ungated)

    @Test
    public void decorate_appendsBareOriginReferrer() throws URISyntaxException {
        final String decorated =
                MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, ORIGIN_PKG);

        final Map<String, String> params = queryParametersOf(decorated);
        assertEquals(CP_ID, params.get("id"));
        assertEquals(ORIGIN_PKG, params.get(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM));
        assertTrue("The Play Store path must be preserved.",
                decorated.startsWith("https://play.google.com/store/apps/details"));
    }

    @Test
    public void decorate_replacesAnyExistingReferrer_soExactlyOneResults() throws URISyntaxException {
        final String linkWithReferrer = CP_APP_LINK + "&referrer=com.some.other.app";

        final String decorated =
                MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(linkWithReferrer, ORIGIN_PKG);

        assertEquals(ORIGIN_PKG,
                queryParametersOf(decorated).get(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM));
        assertEquals("Exactly one referrer must survive.",
                1, countOccurrences(decorated, "referrer="));
    }

    @Test
    public void decorate_missingInputs_returnsOriginalUnchanged() {
        assertNull(MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(null, ORIGIN_PKG));
        assertEquals(CP_APP_LINK,
                MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, null));
        assertEquals(CP_APP_LINK,
                MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, ""));
        assertEquals("", MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer("", ORIGIN_PKG));
    }

    @Test
    public void decorate_unparseableAppLink_returnsOriginalUnchanged() {
        // A broker install must never be broken by referrer decoration.
        final String malformed = "https://play.google.com/store/apps/details?id=x^y|z";

        assertEquals(malformed,
                MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(malformed, ORIGIN_PKG));
    }

    // endregion

    // region decorateAppLinkForMamCaInstall (flight- and marker-gated)

    @Test
    public void gated_flightOnAndMarkerPresent_decorates() {
        setMamCaReferrerFlight(true);

        assertEquals(MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrer(CP_APP_LINK, ORIGIN_PKG),
                MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                        CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()));
    }

    @Test
    public void gated_flightOff_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(false);

        assertEquals(CP_APP_LINK, MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()));
    }

    @Test
    public void gated_flightOnButNotAMamCaInstall_returnsOriginalUnchanged() {
        // An ordinary device-registration broker install must keep behaving exactly as it does today.
        setMamCaReferrerFlight(true);

        final Map<String, String> plainInstall = new HashMap<>();
        plainInstall.put("username", "user@contoso.com");
        plainInstall.put("app_link", CP_APP_LINK);

        assertEquals(CP_APP_LINK, MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, plainInstall));
    }

    @Test
    public void gated_flightOnButMarkerNotEnabledValue_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(true);

        final Map<String, String> disabledMarker = new HashMap<>();
        disabledMarker.put(MamCaRedirect.KEY_INTUNE_APP_PROTECTION, "0");

        assertEquals(CP_APP_LINK, MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, disabledMarker));
    }

    @Test
    public void gated_nullRedirectParameters_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(true);

        assertEquals(CP_APP_LINK,
                MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(CP_APP_LINK, ORIGIN_PKG, null));
    }

    @Test
    public void gated_missingPackage_returnsOriginalUnchanged() {
        setMamCaReferrerFlight(true);

        assertEquals(CP_APP_LINK, MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, null, mamCaRedirectParameters()));
        assertEquals(CP_APP_LINK, MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, "", mamCaRedirectParameters()));
    }

    @Test
    public void gated_noFlightsManager_defaultsOff_returnsOriginalUnchanged() {
        // With no flights manager initialized, the CommonFlight default (false) applies.
        assertEquals(CP_APP_LINK, MamInstallReferrerBuilder.decorateAppLinkForMamCaInstall(
                CP_APP_LINK, ORIGIN_PKG, mamCaRedirectParameters()));
    }

    // endregion

    private static Map<String, String> mamCaRedirectParameters() {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put("username", "user@contoso.com");
        parameters.put("app_link", CP_APP_LINK);
        parameters.put(MamCaRedirect.KEY_INTUNE_APP_PROTECTION,
                MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED);
        return parameters;
    }

    private static Map<String, String> queryParametersOf(final String url) throws URISyntaxException {
        final Map<String, String> parameters = new HashMap<>();
        for (final NameValuePair pair : new CommonURIBuilder(url).getQueryParams()) {
            parameters.put(pair.getName(), pair.getValue());
        }
        return parameters;
    }

    private static int countOccurrences(final String haystack, final String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }

    /** Enables or disables the MAM-CA install referrer flight for the duration of a test. */
    private static void setMamCaReferrerFlight(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.ENABLE_MAM_CA_INSTALL_REFERRER.getKey(),
                Boolean.toString(enabled));
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }
}
