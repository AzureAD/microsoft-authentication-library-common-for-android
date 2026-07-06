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
package com.microsoft.identity.common.internal.ui.browser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Regression guard for the Intune exemption in
 * {@link BrowserAuthorizationStrategy#isIntuneCaller(AuthorizationRequest)}.
 * <p>
 * The exemption exists because, during Intune's system-browser flow, the broker's own
 * {@code BrokerBrowserRedirectActivity} trips the multiple-apps URL-scheme guard and produces a
 * {@code multiple_apps_listening_url_scheme} error (introduced by PR #3070). If someone removes or
 * narrows the exemption — or breaks the {@code <packageName>/<signature>} identifier parsing it
 * relies on — these tests fail, preventing the Intune regression from silently returning.
 */
@RunWith(RobolectricTestRunner.class)
public class BrowserAuthorizationStrategyIntuneExemptionTest {

    private static final String INTUNE_PACKAGE = "com.microsoft.intune";

    /** Real base64 SHA-512 signing signature; note it contains a '/'. */
    private static final String SIGNATURE =
            "jPpMoaNvcxSLMX4yG4C3Gf86rtTqh33SqpuRKg4WOP+MnnpA52zZgvKLW76U4Cqqf68iaBk9W7k/jhciiSAtgQ==";

    private static MicrosoftStsAuthorizationRequest requestWithApplicationIdentifier(final String applicationIdentifier) {
        final MicrosoftStsAuthorizationRequest request = mock(MicrosoftStsAuthorizationRequest.class);
        when(request.getApplicationIdentifier()).thenReturn(applicationIdentifier);
        return request;
    }

    @Test
    public void intuneCaller_isExempted() {
        assertTrue(BrowserAuthorizationStrategy.isIntuneCaller(
                requestWithApplicationIdentifier(INTUNE_PACKAGE + "/" + SIGNATURE)));
    }

    @Test
    public void intuneCaller_isExempted_caseInsensitivePackage() {
        assertTrue(BrowserAuthorizationStrategy.isIntuneCaller(
                requestWithApplicationIdentifier("COM.MICROSOFT.INTUNE/" + SIGNATURE)));
    }

    @Test
    public void nonIntuneCaller_isNotExempted() {
        assertFalse(BrowserAuthorizationStrategy.isIntuneCaller(
                requestWithApplicationIdentifier("com.msft.identity.client.sample.local/" + SIGNATURE)));
    }

    @Test
    public void intunePrefixButDifferentPackage_isNotExempted() {
        // Must be an exact package match, not a prefix, so a look-alike package cannot get the exemption.
        assertFalse(BrowserAuthorizationStrategy.isIntuneCaller(
                requestWithApplicationIdentifier("com.microsoft.intune.evil/" + SIGNATURE)));
    }

    @Test
    public void nullApplicationIdentifier_isNotExempted() {
        assertFalse(BrowserAuthorizationStrategy.isIntuneCaller(
                requestWithApplicationIdentifier(null)));
    }

    @Test
    public void nonMicrosoftStsRequest_isNotExempted() {
        assertFalse(BrowserAuthorizationStrategy.isIntuneCaller(mock(AuthorizationRequest.class)));
    }
}
