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
package com.microsoft.identity.common;

import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsPromptBehavior;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class MicrosoftStsAuthorizationRequestTests {
    private static final String DEFAULT_TEST_RESPONSETYPE = "code";
    private static final String DEFAULT_TEST_CLIENT_ID = "some-client-id";
    private static final String DEFAULT_TEST_REDIRECT_URI = "some://redirect.uri";
    private static final String DEFAULT_TEST_STATE = "someState";
    private static final String DEFAULT_TEST_LOGIN_HINT = "someLoginHint";
    private static final String[] DEFAULT_TEST_SCOPE_ARRAY = {"scope1", "scope2"};
    private static final String[] DEFAULT_TEST_EXTRA_SCOPE_ARRAY = {"scope3", "scope4"};
    private static final Set<String> DEFAULT_TEST_SCOPE = new HashSet<>(Arrays.asList(DEFAULT_TEST_SCOPE_ARRAY));
    private static final String DEFAULT_TEST_AUTHORIZATION_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/authorize";
    private static final UUID DEFAULT_TEST_CORRELATION_ID = UUID.randomUUID();
    private static final String DEFAULT_TEST_EXTRA_QP = "extra=1&haschrome=1";
    private static final String DEFAULT_TEST_VERSION = "0.1";
    private static final MicrosoftStsPromptBehavior DEFAULT_TEST_PROMPT = MicrosoftStsPromptBehavior.CONSENT;
    private static final String DEFAULT_TEST_UID = "1";
    private static final String DEFAULT_TEST_UTID = "1234-5678-90abcdefg";
    private static final String DEFAULT_TEST_DISPLAYABLEID = "user@contoso.com";
    private static final String DEFAULT_TEST_SLICE_PARAMETER = "slice=myslice";
    private static final Set<String> DEFAULT_TEST_EXTRA_SCOPE = new HashSet<>(Arrays.asList(DEFAULT_TEST_EXTRA_SCOPE_ARRAY));
    private static final String DEFAULT_TEST_AUTHORITY_STRING = "https://login.microsoftonline.com/common";

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(DEFAULT_TEST_AUTHORITY_STRING);
    }

    static MicrosoftStsAuthorizationRequest createAuthenticationRequest(final String responseType,
                                                                        @NonNull final String clientId,
                                                                        final String redirectUri,
                                                                        final String state,
                                                                        @NonNull final Set<String> scope,
                                                                        @NonNull final URL authority,
                                                                        @NonNull final String authorizationEndpoint,
                                                                        final String loginHint,
                                                                        final UUID correlationId,
                                                                        final PkceChallenge pkceChallenge,
                                                                        final String extraQueryParam,
                                                                        final String libraryVersion,
                                                                        @NonNull final MicrosoftStsPromptBehavior promptBehavior,
                                                                        final String uid,
                                                                        final String utid,
                                                                        final String displayableId,
                                                                        final String sliceParameters,
                                                                        final Set<String> extraScopesToConsent) {

        return new MicrosoftStsAuthorizationRequest("code", clientId, redirectUri, state, scope, authority,
                authorizationEndpoint, loginHint, correlationId, null, extraQueryParam, libraryVersion,
                promptBehavior, uid, utid, displayableId, sliceParameters, extraScopesToConsent);
    }

    @Test
    public void testGetCodeRequestUrl() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        // With only required parameters.
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(null,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_AUTHORIZATION_ENDPOINT, null,
                null, null, null, null,
                DEFAULT_TEST_PROMPT, null, null, null, null, null);
        final String actualCodeRequestUrl = request.getAuthorizationStartUrl();
        assertTrue("Prompt", actualCodeRequestUrl.contains("&prompt=consent"));
        assertTrue("Auth endpoint", actualCodeRequestUrl.contains(DEFAULT_TEST_AUTHORIZATION_ENDPOINT));
        assertTrue("scope", actualCodeRequestUrl.contains("scope=offline_access%2Bprofile%2Bscope1%2Bopenid%2Bscope2"));
        assertTrue("Client id", actualCodeRequestUrl.contains("client_id=some-client-id"));
    }

    @Test
    public void testGetCodeRequestUrlWithLoginHint() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        // with login hint
        final MicrosoftStsAuthorizationRequest requestWithLoginHint = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_AUTHORIZATION_ENDPOINT, DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, null, DEFAULT_TEST_VERSION,
                DEFAULT_TEST_PROMPT, null, null, null, null, null);
        final String actualCodeRequestUrlWithLoginHint = requestWithLoginHint.getAuthorizationStartUrl();
        assertTrue("Matching login hint", actualCodeRequestUrlWithLoginHint.contains("login_hint=someLoginHint"));
        assertTrue("Matching response type", actualCodeRequestUrlWithLoginHint.contains("response_type=code"));
        assertTrue("Matching correlation id", actualCodeRequestUrlWithLoginHint.contains("&correlation_id=" + DEFAULT_TEST_CORRELATION_ID.toString()));
        assertTrue("Matching library version", actualCodeRequestUrlWithLoginHint.contains("&x-client-Ver=0.1"));
    }

    @Test
    public void testGetCodeRequestUrlWithForceLoginPrompt() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_AUTHORIZATION_ENDPOINT, DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, null, DEFAULT_TEST_VERSION,
                MicrosoftStsPromptBehavior.FORCE_LOGIN, null, null, null, null, null);
        final String actualCodeRequestUrl = request.getAuthorizationStartUrl();
        assertTrue("Prompt", actualCodeRequestUrl.contains("&prompt=login"));
    }

    @Test
    public void testGetCodeRequestUrlWithSelectAccountPrompt() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_AUTHORIZATION_ENDPOINT, DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, null, DEFAULT_TEST_VERSION,
                MicrosoftStsPromptBehavior.SELECT_ACCOUNT, null, null, null, null, null);
        final String actualCodeRequestUrl = request.getAuthorizationStartUrl();
        assertTrue("Prompt", actualCodeRequestUrl.contains("&prompt=select_account"));
        assertTrue("Matching message",
                actualCodeRequestUrl.contains("&x-client-SKU=MSAL.Android"));
        assertTrue("Matching message",
                actualCodeRequestUrl.contains("&x-client-Ver=0.1"));
    }

    @Test
    public void testGetCodeRequestUrlWithExtraQP() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_AUTHORIZATION_ENDPOINT, DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, DEFAULT_TEST_EXTRA_QP, DEFAULT_TEST_VERSION,
                DEFAULT_TEST_PROMPT, null, null, null, DEFAULT_TEST_SLICE_PARAMETER, null);
        final String actualCodeRequestUrl = request.getAuthorizationStartUrl();
        assertTrue("Extra parameters 1", actualCodeRequestUrl.contains("&extra=1"));
        assertTrue("Extra parameters 2", actualCodeRequestUrl.contains("&haschrome=1"));
        assertTrue("Slice parameters 2", actualCodeRequestUrl.contains("&slice=myslice"));
        assertTrue("Prompt", actualCodeRequestUrl.contains("&prompt=consent"));
    }

    @Test
    public void testGetCodeRequestUrlWithClaim() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        try {
            final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                    null, DEFAULT_TEST_REDIRECT_URI, null, null,
                    getValidRequestUrl(), null, DEFAULT_TEST_LOGIN_HINT,
                    DEFAULT_TEST_CORRELATION_ID, null, DEFAULT_TEST_EXTRA_QP, DEFAULT_TEST_VERSION,
                    DEFAULT_TEST_PROMPT, null, null, null, DEFAULT_TEST_SLICE_PARAMETER, null);
        } catch (final Exception exception) {
            assertTrue(exception instanceof IllegalArgumentException);
            assertTrue(exception.getMessage().contains("clientId is empty"));
        }
    }
}
