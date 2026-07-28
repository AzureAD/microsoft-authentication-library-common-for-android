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

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;

import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link MamCaRedirect} - reading the MAM Conditional Access markers off the
 * broker-install redirect.
 */
public class MamCaRedirectTest {

    private static final String UPN = "user@contoso.com";

    @Test
    public void hasMarker_onlyWhenExplicitlyEnabled() {
        assertTrue(MamCaRedirect.hasIntuneAppProtectionMarker(markedRedirect()));

        assertFalse(MamCaRedirect.hasIntuneAppProtectionMarker(null));
        assertFalse(MamCaRedirect.hasIntuneAppProtectionMarker(new HashMap<String, String>()));
        assertFalse(MamCaRedirect.hasIntuneAppProtectionMarker(redirectWithMarkerValue("0")));
        assertFalse(MamCaRedirect.hasIntuneAppProtectionMarker(redirectWithMarkerValue("true")));
        assertFalse(MamCaRedirect.hasIntuneAppProtectionMarker(redirectWithMarkerValue("")));
    }

    @Test
    public void hasMarker_ignoresTheWithoutMarkerFlight() {
        // hasIntuneAppProtectionMarker reports what the server actually sent, so it must be
        // unaffected by the rollout escape hatch.
        setWithoutMarkerFlight(true);

        assertFalse(MamCaRedirect.hasIntuneAppProtectionMarker(new HashMap<String, String>()));
    }

    @Test
    public void isMamCaInstall_markerPresent_isTrue() {
        assertTrue(MamCaRedirect.isMamCaInstall(markedRedirect()));
    }

    @Test
    public void isMamCaInstall_noMarker_isFalseByDefault() {
        // An ordinary device-registration broker install must not pick up MAM-CA behaviors.
        final Map<String, String> plainInstall = new HashMap<>();
        plainInstall.put(MamCaRedirect.KEY_USERNAME, UPN);

        assertFalse(MamCaRedirect.isMamCaInstall(plainInstall));
        assertFalse(MamCaRedirect.isMamCaInstall(null));
    }

    @Test
    public void isMamCaInstall_noMarker_butWithoutMarkerFlightOn_isTrue() {
        setWithoutMarkerFlight(true);

        assertTrue(MamCaRedirect.isMamCaInstall(new HashMap<String, String>()));
        assertTrue(MamCaRedirect.isMamCaInstall(null));
    }

    @Test
    public void getUsername_returnsUpnOrNull() {
        assertEquals(UPN, MamCaRedirect.getUsername(markedRedirect()));

        assertNull(MamCaRedirect.getUsername(null));
        assertNull(MamCaRedirect.getUsername(new HashMap<String, String>()));

        final Map<String, String> blankUsername = new HashMap<>();
        blankUsername.put(MamCaRedirect.KEY_USERNAME, "");
        assertNull(MamCaRedirect.getUsername(blankUsername));
    }

    @Test
    public void logRedirectParameterNames_isNullSafe() {
        MamCaRedirect.logRedirectParameterNames("test", null);
        MamCaRedirect.logRedirectParameterNames("test", markedRedirect());
    }

    private static Map<String, String> markedRedirect() {
        final Map<String, String> parameters =
                redirectWithMarkerValue(MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED);
        parameters.put(MamCaRedirect.KEY_USERNAME, UPN);
        return parameters;
    }

    private static Map<String, String> redirectWithMarkerValue(final String value) {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(MamCaRedirect.KEY_INTUNE_APP_PROTECTION, value);
        return parameters;
    }

    private static void setWithoutMarkerFlight(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.ENABLE_MAM_CA_INSTALL_WITHOUT_MARKER.getKey(),
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
