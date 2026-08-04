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
package com.microsoft.identity.common.java.commands.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

/**
 * Unit tests for the silent-caller validation gate in {@link BrokerSilentTokenCommandParameters#validate()}.
 *
 * <p>Shape C moved the enforcement decision into this (common4j) {@code validate()}: when the
 * {@link CommonFlight#VALIDATE_SILENT_CALLER} flight is enabled (default), it delegates to
 * {@link IPlatformUtil#validateCallingAppForUid} with the request's {@code callerUid} (which the broker
 * entry points overwrite with the kernel-attested {@code Binder.getCallingUid()}) and
 * {@code callerPackageName}, before the redirect-URI check. These tests verify that <em>wiring</em> against
 * a mock {@link IPlatformUtil} — the platform-specific {@code getPackagesForUid} membership behavior is
 * covered separately by {@code AndroidPlatformUtilTest} against the real implementation.
 */
@RunWith(JUnit4.class)
public class BrokerSilentTokenCommandParametersTest {

    private static final int CALLER_UID = 20001;
    private static final String CALLER_PACKAGE = "com.test.callerapp";
    private static final String REDIRECT_URI = "msauth://com.test.callerapp/signature";

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    /**
     * Flight on + validateCallingAppForUid accepts: the gate calls the platform util with the request's
     * {@code callerUid} and {@code callerPackageName}, then validate() completes without throwing.
     */
    @Test
    public void validate_flightOn_callerAccepted_delegatesWithCallerUidAndPackage() throws Exception {
        setValidateSilentCallerFlight(true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(true);
        doNothing().when(platformUtil).validateCallingAppForUid(anyInt(), anyString());

        final BrokerSilentTokenCommandParameters params = params(platformUtil);
        params.validate();

        verify(platformUtil).validateCallingAppForUid(eq(CALLER_UID), eq(CALLER_PACKAGE));
    }

    /**
     * Flight on + validateCallingAppForUid rejects: validate() propagates the {@code UNKNOWN_CALLER}
     * {@link ClientException} (a spoofed caller is rejected before token issuance).
     */
    @Test
    public void validate_flightOn_callerRejected_throwsUnknownCaller() {
        setValidateSilentCallerFlight(true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        try {
            when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(true);
            doThrow(new ClientException(ErrorStrings.UNKNOWN_CALLER, "spoofed"))
                    .when(platformUtil).validateCallingAppForUid(anyInt(), anyString());

            params(platformUtil).validate();
            fail("Expected ClientException(UNKNOWN_CALLER) to propagate from the caller-validation gate.");
        } catch (final ClientException e) {
            assertEquals(ErrorStrings.UNKNOWN_CALLER, e.getErrorCode());
        } catch (final ArgumentException e) {
            fail("Expected ClientException(UNKNOWN_CALLER), not ArgumentException: " + e.getMessage());
        }
    }

    /**
     * Flight off (kill-switch): the caller-validation gate is skipped entirely — validateCallingAppForUid is
     * never invoked even for a caller that would otherwise be rejected, restoring the pre-fix behavior.
     */
    @Test
    public void validate_flightOff_callerCheckSkipped() throws Exception {
        setValidateSilentCallerFlight(false);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(true);
        // Would reject if consulted; the flight-off gate must not consult it.
        doThrow(new ClientException(ErrorStrings.UNKNOWN_CALLER, "spoofed"))
                .when(platformUtil).validateCallingAppForUid(anyInt(), anyString());

        params(platformUtil).validate();

        verify(platformUtil, never()).validateCallingAppForUid(anyInt(), anyString());
    }

    /**
     * Sanity: the gate reads {@code getCallerUid()} (the trusted uid), not some other field. A params
     * object whose callerUid differs from a stale value still forwards callerUid to the platform util.
     */
    @Test
    public void validate_flightOn_forwardsGetCallerUid() throws Exception {
        setValidateSilentCallerFlight(true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(true);

        final BrokerSilentTokenCommandParameters params = params(platformUtil);
        assertEquals("Precondition: params must expose the configured caller uid.",
                CALLER_UID, params.getCallerUid());

        params.validate();

        verify(platformUtil).validateCallingAppForUid(eq(params.getCallerUid()), eq(CALLER_PACKAGE));
    }

    // ---- helpers ------------------------------------------------------------------------------

    /**
     * Builds a minimal but valid {@link BrokerSilentTokenCommandParameters} that reaches the silent-caller
     * gate: every earlier {@code validate()} precondition (callerUid, authority, scopes, clientId, broker
     * cache, broker account) is satisfied, {@code requestType} is left non-WebApps so the WebApps early
     * return does not apply, and the platform util is the supplied mock.
     */
    private BrokerSilentTokenCommandParameters params(final IPlatformUtil platformUtil) {
        final IPlatformComponents components = mock(IPlatformComponents.class);
        when(components.getPlatformUtil()).thenReturn(platformUtil);

        return BrokerSilentTokenCommandParameters.builder()
                .platformComponents(components)
                .oAuth2TokenCache(mock(BrokerOAuth2TokenCache.class))
                .authority(mock(Authority.class))
                .scopes(Collections.singleton("User.Read"))
                .clientId("11111111-1111-1111-1111-111111111111")
                .redirectUri(REDIRECT_URI)
                .callerUid(CALLER_UID)
                .callerPackageName(CALLER_PACKAGE)
                .brokerAccount(mock(IBrokerAccount.class))
                .build();
    }

    private void setValidateSilentCallerFlight(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.VALIDATE_SILENT_CALLER.getKey(), Boolean.toString(enabled));

        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);

        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }
}
