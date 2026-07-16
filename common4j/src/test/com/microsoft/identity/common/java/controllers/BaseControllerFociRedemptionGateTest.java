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
package com.microsoft.identity.common.java.controllers;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;

import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.foci.FociQueryUtilities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for the silent shared-FoCI refresh-token redemption gate reached via
 * {@link BaseController#getCachedAccountRecordFromAllCaches} (implemented in the private
 * {@code getAccountWithFRTIfAvailable}) — AB#3687466.
 *
 * <p>The gate skips redemption of the device-wide shared family refresh token when the caller is not
 * authorized to share FoCI tokens ({@code callerAuthorizedForFoci == false}), falling through to the
 * caller's own UID-partitioned cache (returns {@code null}) instead of calling
 * {@link FociQueryUtilities#tryFociTokenWithGivenClientId}.
 */
@RunWith(JUnit4.class)
public class BaseControllerFociRedemptionGateTest {

    private static final String HOME_ACCOUNT_ID = "home.account.id";
    private static final String LOCAL_ACCOUNT_ID = "local.account.id";
    private static final String CLIENT_ID = "test-client-id";
    private static final String REDIRECT_URI = "msauth://redirect";

    private static SilentTokenCommandParameters paramsWithSharedFociRt(final boolean callerAuthorizedForFoci,
                                                                       final MsalOAuth2TokenCache cache) {
        // A shared family refresh token exists for the home account id.
        Mockito.when(cache.getFamilyRefreshTokenForHomeAccountId(HOME_ACCOUNT_ID))
                .thenReturn(new RefreshTokenRecord());

        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);

        final SilentTokenCommandParameters parameters = Mockito.mock(SilentTokenCommandParameters.class);
        Mockito.when(parameters.getOAuth2TokenCache()).thenReturn(cache);
        Mockito.when(parameters.getAccount()).thenReturn(account);
        Mockito.when(parameters.getClientId()).thenReturn(CLIENT_ID);
        Mockito.when(parameters.getRedirectUri()).thenReturn(REDIRECT_URI);
        Mockito.when(parameters.isCallerAuthorizedForFoci()).thenReturn(callerAuthorizedForFoci);
        return parameters;
    }

    @Test
    public void getCachedAccountRecordFromAllCaches_callerNotAuthorizedForFoci_skipsFrtRedemption()
            throws Exception {
        final MsalOAuth2TokenCache cache = Mockito.mock(MsalOAuth2TokenCache.class);
        final SilentTokenCommandParameters parameters = paramsWithSharedFociRt(false, cache);
        final BaseController controller = Mockito.mock(BaseController.class, Mockito.CALLS_REAL_METHODS);

        try (final MockedStatic<FociQueryUtilities> foci = Mockito.mockStatic(FociQueryUtilities.class)) {
            final AccountRecord result = controller.getCachedAccountRecordFromAllCaches(parameters);

            assertNull(result);
            // The shared FoCI refresh token must NOT be redeemed for an unauthorized caller.
            foci.verify(
                    () -> FociQueryUtilities.tryFociTokenWithGivenClientId(any(), any(), any(), any(), any()),
                    Mockito.never());
        }
    }

    @Test
    public void getCachedAccountRecordFromAllCaches_callerAuthorizedForFoci_attemptsFrtRedemption()
            throws Exception {
        final MsalOAuth2TokenCache cache = Mockito.mock(MsalOAuth2TokenCache.class);
        final SilentTokenCommandParameters parameters = paramsWithSharedFociRt(true, cache);
        final BaseController controller = Mockito.mock(BaseController.class, Mockito.CALLS_REAL_METHODS);

        try (final MockedStatic<FociQueryUtilities> foci = Mockito.mockStatic(FociQueryUtilities.class)) {
            controller.getCachedAccountRecordFromAllCaches(parameters);

            // An authorized caller proceeds to redeem the shared FoCI refresh token (pre-fix behavior).
            foci.verify(
                    () -> FociQueryUtilities.tryFociTokenWithGivenClientId(any(), any(), any(), any(), any()),
                    Mockito.times(1));
        }
    }
}
