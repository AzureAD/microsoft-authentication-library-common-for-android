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

import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link MamCaRedirect} - reading the MAM Conditional Access marker off the
 * broker-install redirect.
 */
public class MamCaRedirectTest {

    private static final String UPN = "user@contoso.com";

    @Test
    public void isMamCaInstall_onlyWhenMarkerIsExactlyOne() {
        assertTrue(MamCaRedirect.isMamCaInstall(markedRedirect()));

        assertFalse(MamCaRedirect.isMamCaInstall(null));
        assertFalse(MamCaRedirect.isMamCaInstall(new HashMap<String, String>()));
        assertFalse(MamCaRedirect.isMamCaInstall(redirectWithMarkerValue("0")));
        assertFalse(MamCaRedirect.isMamCaInstall(redirectWithMarkerValue("true")));
        assertFalse(MamCaRedirect.isMamCaInstall(redirectWithMarkerValue("")));
    }

    @Test
    public void isMamCaInstall_plainBrokerInstall_isFalse() {
        // An ordinary device-registration broker install must not pick up MAM-CA behaviors.
        final Map<String, String> plainInstall = new HashMap<>();
        plainInstall.put(MamCaRedirect.KEY_USERNAME, UPN);

        assertFalse(MamCaRedirect.isMamCaInstall(plainInstall));
    }

    @Test
    public void isMamCaInstall_readsTheServerInstallLinkShape() {
        // Mirrors what the service appends to the broker-install link when it fails the request
        // with AADSTS50127 for MAM: intuneAppProtection is a top-level parameter alongside
        // app_link, not something nested inside it.
        final Map<String, String> redirect = new HashMap<>();
        redirect.put("wpj", "1");
        redirect.put(MamCaRedirect.KEY_USERNAME, UPN);
        redirect.put("app_link",
                "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal");
        redirect.put(MamCaRedirect.KEY_INTUNE_APP_PROTECTION,
                MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED);

        assertTrue(MamCaRedirect.isMamCaInstall(redirect));
        assertEquals(UPN, MamCaRedirect.getUsername(redirect));
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

    /**
     * A trailing token with no {@code =} is parsed as a key with no value, so a malformed redirect
     * can present a UPN as a parameter name. Names are logged on the non-PII channel, so anything
     * that is not shaped like a parameter name has to be held back.
     */
    @Test
    public void printableParameterNames_withholdsAnythingThatIsNotAName() {
        final Set<String> keys = new HashSet<>(Arrays.asList(
                "app_link",
                MamCaRedirect.KEY_INTUNE_APP_PROTECTION,
                MamCaRedirect.KEY_USERNAME,
                UPN,
                "someone else@contoso.com"));

        final Set<String> printable = MamCaRedirect.printableParameterNames(keys);

        assertTrue(printable.contains("app_link"));
        assertTrue(printable.contains(MamCaRedirect.KEY_INTUNE_APP_PROTECTION));
        assertTrue(printable.contains(MamCaRedirect.KEY_USERNAME));
        assertFalse("a UPN must never reach the log line", printable.contains(UPN));
        assertFalse(printable.contains("someone else@contoso.com"));
        assertEquals(3, printable.size());
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
}
