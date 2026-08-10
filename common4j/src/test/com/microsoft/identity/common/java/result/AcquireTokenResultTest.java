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
package com.microsoft.identity.common.java.result;

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.ClientDataInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AcquireTokenResultTest {

    private static final String PIPE_DELIMITED_A = "m|AADSTS50058|login_required|us|public";
    private static final String PIPE_DELIMITED_B = "e|AADSTS70011|invalid_scope|eu|sovereign";
    private static final String PIPE_DELIMITED_C = "m|AADSTS50076|mfa_required|us|public";

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private ClientDataInfo makeClientDataInfo(final String raw) {
        return ClientDataInfo.fromPipeDelimited(raw);
    }

    private LocalAuthenticationResult makeLocalAuthResult(final ClientDataInfo clientDataInfo) {
        final LocalAuthenticationResult localAuthResult = mock(LocalAuthenticationResult.class);
        when(localAuthResult.getClientDataInfo()).thenReturn(clientDataInfo);
        return localAuthResult;
    }

    private MicrosoftStsAuthorizationResult makeAuthResult(final ClientDataInfo clientDataInfo) {
        final MicrosoftStsAuthorizationResult authResult = new MicrosoftStsAuthorizationResult(
                AuthorizationStatus.SUCCESS, (MicrosoftStsAuthorizationResponse) null);
        authResult.setClientDataInfo(clientDataInfo);
        return authResult;
    }

    // ---------------------------------------------------------------------------
    // No sources populated
    // ---------------------------------------------------------------------------

    @Test
    public void getClientDataInfo_noSources_returnsNull() {
        final AcquireTokenResult result = new AcquireTokenResult();
        assertNull(result.getClientDataInfo());
    }

    // ---------------------------------------------------------------------------
    // LocalAuthenticationResult source
    // ---------------------------------------------------------------------------

    @Test
    public void getClientDataInfo_localAuthResultHasData_returnsIt() {
        final ClientDataInfo expected = makeClientDataInfo(PIPE_DELIMITED_A);
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setLocalAuthenticationResult(makeLocalAuthResult(expected));

        assertSame(expected, result.getClientDataInfo());
    }

    @Test
    public void getClientDataInfo_localAuthResultHasNull_fallsBackToTokenResult() {
        final ClientDataInfo tokenData = makeClientDataInfo(PIPE_DELIMITED_A);
        final TokenResult tokenResult = new TokenResult();
        tokenResult.setClientDataInfo(tokenData);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setLocalAuthenticationResult(makeLocalAuthResult(null));
        result.setTokenResult(tokenResult);

        assertSame(tokenData, result.getClientDataInfo());
    }

    // ---------------------------------------------------------------------------
    // TokenResult fallback
    // ---------------------------------------------------------------------------

    @Test
    public void getClientDataInfo_tokenResultHasData_returnsIt() {
        final ClientDataInfo expected = makeClientDataInfo(PIPE_DELIMITED_A);
        final TokenResult tokenResult = new TokenResult();
        tokenResult.setClientDataInfo(expected);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setTokenResult(tokenResult);

        assertSame(expected, result.getClientDataInfo());
    }

    @Test
    public void getClientDataInfo_tokenResultHasNull_fallsBackToAuthResult() {
        final ClientDataInfo authData = makeClientDataInfo(PIPE_DELIMITED_A);
        final TokenResult tokenResult = new TokenResult();
        tokenResult.setClientDataInfo(null);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setTokenResult(tokenResult);
        result.setAuthorizationResult(makeAuthResult(authData));

        assertSame(authData, result.getClientDataInfo());
    }

    @Test
    public void getClientDataInfo_noTokenResult_fallsBackToAuthResult() {
        final ClientDataInfo authData = makeClientDataInfo(PIPE_DELIMITED_A);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setAuthorizationResult(makeAuthResult(authData));

        assertSame(authData, result.getClientDataInfo());
    }

    // ---------------------------------------------------------------------------
    // MicrosoftStsAuthorizationResult fallback
    // ---------------------------------------------------------------------------

    @Test
    public void getClientDataInfo_authResultHasData_returnsIt() {
        final ClientDataInfo expected = makeClientDataInfo(PIPE_DELIMITED_A);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setAuthorizationResult(makeAuthResult(expected));

        assertSame(expected, result.getClientDataInfo());
    }

    @Test
    public void getClientDataInfo_authResultHasNull_returnsNull() {
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setAuthorizationResult(makeAuthResult(null));

        assertNull(result.getClientDataInfo());
    }

    // ---------------------------------------------------------------------------
    // Precedence: LocalAuthResult > TokenResult > AuthResult
    // ---------------------------------------------------------------------------

    @Test
    public void getClientDataInfo_allSourcesPopulated_prefersLocalAuthResult() {
        final ClientDataInfo localData = makeClientDataInfo(PIPE_DELIMITED_A);
        final ClientDataInfo tokenData = makeClientDataInfo(PIPE_DELIMITED_B);
        final ClientDataInfo authData = makeClientDataInfo(PIPE_DELIMITED_C);

        final TokenResult tokenResult = new TokenResult();
        tokenResult.setClientDataInfo(tokenData);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setLocalAuthenticationResult(makeLocalAuthResult(localData));
        result.setTokenResult(tokenResult);
        result.setAuthorizationResult(makeAuthResult(authData));

        assertSame(localData, result.getClientDataInfo());
    }

    @Test
    public void getClientDataInfo_localAuthNullTokenPopulated_prefersTokenResult() {
        final ClientDataInfo tokenData = makeClientDataInfo(PIPE_DELIMITED_A);
        final ClientDataInfo authData = makeClientDataInfo(PIPE_DELIMITED_B);

        final TokenResult tokenResult = new TokenResult();
        tokenResult.setClientDataInfo(tokenData);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setLocalAuthenticationResult(makeLocalAuthResult(null));
        result.setTokenResult(tokenResult);
        result.setAuthorizationResult(makeAuthResult(authData));

        assertSame(tokenData, result.getClientDataInfo());
    }

    @Test
    public void getClientDataInfo_localAuthNullTokenNull_returnsAuthResult() {
        final ClientDataInfo authData = makeClientDataInfo(PIPE_DELIMITED_A);

        final TokenResult tokenResult = new TokenResult();
        tokenResult.setClientDataInfo(null);

        final AcquireTokenResult result = new AcquireTokenResult();
        result.setLocalAuthenticationResult(makeLocalAuthResult(null));
        result.setTokenResult(tokenResult);
        result.setAuthorizationResult(makeAuthResult(authData));

        assertSame(authData, result.getClientDataInfo());
    }

    // ---------------------------------------------------------------------------
    // Onboarding blob accessor tests
    // ---------------------------------------------------------------------------

    @Test
    public void onboardingBlob_DefaultsToNull() {
        final AcquireTokenResult result = new AcquireTokenResult();
        Assert.assertNull(result.getOnboardingBlob());
    }

    @Test
    public void onboardingBlob_RoundTripsThroughSetter() {
        final String blobJson = "{\"schema_version\":\"1.0.0\","
                + "\"session_correlation_id\":\"abc-123\","
                + "\"onboarding_mode\":\"brokered\"}";
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setOnboardingBlob(blobJson);
        Assert.assertEquals(blobJson, result.getOnboardingBlob());
    }

    @Test
    public void onboardingBlob_NullSetterClearsValue() {
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setOnboardingBlob("non-null");
        result.setOnboardingBlob(null);
        Assert.assertNull(result.getOnboardingBlob());
    }
}
