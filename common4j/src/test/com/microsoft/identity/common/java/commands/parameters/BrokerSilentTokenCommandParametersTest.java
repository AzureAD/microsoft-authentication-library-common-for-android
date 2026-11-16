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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.request.BrokerRequestType;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

/**
 * Unit tests for silent token command preconditions after native caller validation moved
 * to the Broker request boundary. WEB_APPS retains its dedicated platform validation.
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

    @Test
    public void validate_nativeRequestDoesNotRepeatCallerCheckRegardlessOfFlight() throws Exception {
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        for (final boolean enabled : new boolean[]{true, false}) {
            setFlight(CommonFlight.VALIDATE_SILENT_CALLER, enabled);
            params(platformUtil).validate();
        }
        verifyNoInteractions(platformUtil);
    }

    @Test
    public void validate_nativeRequestRetainsParameterPreconditions() throws Exception {
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        final BrokerSilentTokenCommandParameters valid = params(platformUtil);
        assertInvalid(valid.toBuilder().callerUid(0).build());
        assertInvalid(valid.toBuilder().authority(null).build());
        assertInvalid(valid.toBuilder().scopes(Collections.emptySet()).build());
        assertInvalid(valid.toBuilder().clientId("").build());
        assertInvalid(valid.toBuilder().oAuth2TokenCache(null).build());
        assertInvalid(valid.toBuilder().brokerAccount(null).build());
        verifyNoInteractions(platformUtil);
    }

    @Test
    public void validate_webAppsUsesDedicatedCallerCheck() throws Exception {
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        params(platformUtil).toBuilder().requestType(BrokerRequestType.WEB_APPS).build().validate();
        verify(platformUtil).isValidCallingAppForWebApps(eq(CALLER_UID));
    }

    @Test
    public void validate_disabledWebAppsDoesNotInvokeDedicatedCallerCheck() throws Exception {
        setFlight(CommonFlight.DISABLE_WEB_APPS_API, true);
        final IPlatformUtil platformUtil = mock(IPlatformUtil.class);
        params(platformUtil).toBuilder().requestType(BrokerRequestType.WEB_APPS).build().validate();
        verifyNoInteractions(platformUtil);
    }

    private void assertInvalid(final BrokerSilentTokenCommandParameters parameters) throws ClientException {
        try {
            parameters.validate();
            fail("Expected invalid token command parameters to be rejected.");
        } catch (final ArgumentException expected) {
            // Parameter preconditions remain in Common even though caller admission moved to Broker.
        }
    }

    /**
     * Builds valid parameters for the broker request after caller admission.
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

    private void setFlight(final CommonFlight flight, final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(flight.getKey(), Boolean.toString(enabled));

        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);

        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }
}
