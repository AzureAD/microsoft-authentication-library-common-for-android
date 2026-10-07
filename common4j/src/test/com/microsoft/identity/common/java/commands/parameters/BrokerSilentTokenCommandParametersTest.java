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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.flighting.SilentCallerValidationFlights;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.request.BrokerRequestType;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for coupled silent-caller flights and trusted broker-passthrough validation.
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
     * Only the combination of both flights enables caller UID/package ownership validation.
     */
    @Test
    public void validate_flightMatrix_enablesOwnershipOnlyWhenBothFlightsAreOn() throws Exception {
        assertEquals(Boolean.TRUE, CommonFlight.ENABLE_SILENT_CALLER_PASSTHROUGH_VALIDATION.getDefaultValue());
        for (final boolean validateSilentCaller : new boolean[]{false, true}) {
            for (final boolean passthroughValidation : new boolean[]{false, true}) {
                setFlights(validateSilentCaller, passthroughValidation);
                final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
                when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(true);
                when(platformUtil.isValidCallingApp(anyString(), anyString(), anyInt())).thenReturn(true);

                final boolean expectedOwnershipValidation = validateSilentCaller && passthroughValidation;
                assertEquals(expectedOwnershipValidation, SilentCallerValidationFlights.isCompleteSolutionEnabled());
                params(platformUtil).validate();

                if (expectedOwnershipValidation) {
                    verify(platformUtil).isValidCallingApp(
                            eq(REDIRECT_URI), eq(CALLER_PACKAGE), eq(CALLER_UID));
                    verify(platformUtil, never()).isValidCallingApp(anyString(), anyString());
                } else {
                    verify(platformUtil).isValidCallingApp(eq(REDIRECT_URI), eq(CALLER_PACKAGE));
                    verify(platformUtil, never()).isValidCallingApp(anyString(), anyString(), anyInt());
                }
            }
        }
    }

    /**
     * Direct validation uses the ownership-aware overload when both flights are enabled.
     */
    @Test
    public void validate_bothFlightsOnAndCallerAccepted_usesOwnershipValidation() throws Exception {
        setFlights(true, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        when(platformUtil.isValidCallingApp(anyString(), anyString(), anyInt())).thenReturn(true);

        params(platformUtil).validate();

        verify(platformUtil).isValidCallingApp(eq(REDIRECT_URI), eq(CALLER_PACKAGE), eq(CALLER_UID));
    }

    /**
     * Direct validation preserves the shared platform argument error when both flights are enabled.
     */
    @Test
    public void validate_bothFlightsOnAndCallerRejected_throwsArgumentException() {
        setFlights(true, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        try {
            when(platformUtil.isValidCallingApp(anyString(), anyString(), anyInt()))
                    .thenThrow(new ArgumentException(
                            ArgumentException.BROKER_TOKEN_REQUEST_OPERATION_NAME,
                            ArgumentException.CALLER_PACKAGE_NAME_ARGUMENT_NAME,
                            "spoofed"));

            params(platformUtil).validate();
            fail("Expected ArgumentException to propagate from the caller-validation gate.");
        } catch (final ArgumentException e) {
            assertEquals(ArgumentException.BROKER_TOKEN_REQUEST_OPERATION_NAME, e.getOperationName());
            assertEquals(ArgumentException.CALLER_PACKAGE_NAME_ARGUMENT_NAME, e.getArgumentName());
        } catch (final ClientException e) {
            fail("Expected ArgumentException, not ClientException: " + e.getMessage());
        }
        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString());
    }

    /**
     * Trusted passthrough skips UID/package ownership but still validates the redirect URI.
     */
    @Test
    public void validateForTrustedBrokerPassthrough_skipsOwnershipButChecksRedirect() throws Exception {
        setFlights(true, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(true);
        when(platformUtil.isValidCallingApp(anyString(), anyString(), anyInt()))
                .thenThrow(new ArgumentException(
                        ArgumentException.BROKER_TOKEN_REQUEST_OPERATION_NAME,
                        ArgumentException.CALLER_PACKAGE_NAME_ARGUMENT_NAME,
                        "spoofed"));

        params(platformUtil).validateForTrustedBrokerPassthrough();

        verify(platformUtil).isValidCallingApp(eq(REDIRECT_URI), eq(CALLER_PACKAGE));
        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString(), anyInt());
    }

    /**
     * A failed redirect check remains an error for trusted passthrough callers.
     */
    @Test
    public void validateForTrustedBrokerPassthrough_redirectRejected_throwsArgumentException() throws Exception {
        setFlights(true, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        when(platformUtil.isValidCallingApp(anyString(), anyString())).thenReturn(false);

        try {
            params(platformUtil).validateForTrustedBrokerPassthrough();
            fail("Expected redirect validation to reject the request.");
        } catch (final ArgumentException expected) {
            assertEquals("mRedirectUri", expected.getArgumentName());
        }

        verify(platformUtil).isValidCallingApp(eq(REDIRECT_URI), eq(CALLER_PACKAGE));
        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString(), anyInt());
    }

    /**
     * Trusted passthrough retains all parameter preconditions that run before caller/redirect validation.
     */
    @Test
    public void validateForTrustedBrokerPassthrough_invalidParametersStillFail()
            throws ClientException, ArgumentException {
        setFlights(true, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        final BrokerSilentTokenCommandParameters validParams = params(platformUtil);
        final List<BrokerSilentTokenCommandParameters> invalidParams = new ArrayList<>();
        invalidParams.add(validParams.toBuilder().callerUid(0).build());
        invalidParams.add(validParams.toBuilder().authority(null).build());
        invalidParams.add(validParams.toBuilder().scopes(Collections.emptySet()).build());
        invalidParams.add(validParams.toBuilder().clientId("").build());
        invalidParams.add(validParams.toBuilder().oAuth2TokenCache(null).build());
        invalidParams.add(validParams.toBuilder().brokerAccount(null).build());

        for (final BrokerSilentTokenCommandParameters invalid : invalidParams) {
            assertTrustedValidationFails(invalid);
        }

        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString());
        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString(), anyInt());
    }

    /**
     * WEB_APPS retains its existing dedicated caller validation and early return.
     */
    @Test
    public void validateForTrustedBrokerPassthrough_webAppsStillUsesWebAppsValidation() throws Exception {
        setFlights(true, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);

        params(platformUtil).toBuilder()
                .requestType(BrokerRequestType.WEB_APPS)
                .build()
                .validateForTrustedBrokerPassthrough();

        verify(platformUtil).isValidCallingAppForWebApps(CALLER_UID);
        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString());
        verify(platformUtil, never()).isValidCallingApp(anyString(), anyString(), anyInt());
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

    private void assertTrustedValidationFails(final BrokerSilentTokenCommandParameters params)
            throws ClientException {
        try {
            params.validateForTrustedBrokerPassthrough();
            fail("Expected invalid request parameters to fail validation.");
        } catch (final ArgumentException expected) {
            // Expected: trusted passthrough retains regular request validation.
        }
    }

    private void setFlights(final boolean validateSilentCaller, final boolean passthroughValidation) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(
                CommonFlight.VALIDATE_SILENT_CALLER.getKey(),
                Boolean.toString(validateSilentCaller));
        provider.addFlight(
                CommonFlight.ENABLE_SILENT_CALLER_PASSTHROUGH_VALIDATION.getKey(),
                Boolean.toString(passthroughValidation));

        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);

        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }
}
