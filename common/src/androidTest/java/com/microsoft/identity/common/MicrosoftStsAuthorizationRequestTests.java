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

import android.net.Uri;
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
    private static final String DEFAULT_TEST_SCOPE = "scope1 scope2";
    private static final String DEFAULT_TEST_EXTRA_SCOPE = "scope3 scope4";
    private static final String DEFAULT_TEST_AUTHORIZATION_ENDPOINT = "https://login.microsoftonline.com/common/oauth2/authorize";
    private static final UUID DEFAULT_TEST_CORRELATION_ID = UUID.randomUUID();
    private static final String DEFAULT_TEST_EXTRA_QP = "extra=1&haschrome=1";
    private static final String DEFAULT_TEST_VERSION = "0.1";
    private static final String DEFAULT_TEST_PROMPT = MicrosoftStsAuthorizationRequest.Prompt.CONSENT;
    private static final String DEFAULT_TEST_UID = "1";
    private static final String DEFAULT_TEST_UTID = "1234-5678-90abcdefg";
    private static final String DEFAULT_TEST_DISPLAYABLEID = "user@contoso.com";
    private static final String DEFAULT_TEST_SLICE_PARAMETER = "slice=myslice";
    private static final String DEFAULT_TEST_AUTHORITY_STRING = "https://login.microsoftonline.com/common";
    private static final String DEFAULT_TEST_LIBRARY_NAME = "some_library_name";

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(DEFAULT_TEST_AUTHORITY_STRING);
    }

    static MicrosoftStsAuthorizationRequest createAuthenticationRequest(final String responseType,
                                                                        @NonNull final String clientId,
                                                                        final String redirectUri,
                                                                        final String state,
                                                                        @NonNull final String scope,
                                                                        @NonNull final URL authority,
                                                                        final String loginHint,
                                                                        final UUID correlationId,
                                                                        final PkceChallenge pkceChallenge,
                                                                        final String extraQueryParam,
                                                                        final String libraryVersion,
                                                                        @NonNull final String promptBehavior,
                                                                        final String uid,
                                                                        final String utid,
                                                                        final String displayableId,
                                                                        final String sliceParameters,
                                                                        final String libraryName) {
        MicrosoftStsAuthorizationRequest.Builder builder = new MicrosoftStsAuthorizationRequest.Builder<MicrosoftStsAuthorizationRequest>(clientId, redirectUri, authority, scope, promptBehavior, pkceChallenge, state);
        builder.setLoginHint(loginHint);
        builder.setCorrelationId(correlationId);
        builder.setExtraQueryParam(extraQueryParam);
        builder.setLibraryVersion(libraryVersion);
        builder.setUid(uid);
        builder.setUtid(utid);
        builder.setDisplayableId(displayableId);
        builder.setSliceParameters(sliceParameters);
        builder.setLibraryName(libraryName);

        return builder.build();
    }


    @Test
    public void testGetCodeRequestUrlWithLoginHint() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        // with login hint
        final MicrosoftStsAuthorizationRequest requestWithLoginHint = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, null, DEFAULT_TEST_VERSION,
                DEFAULT_TEST_PROMPT, null, null, null, null, DEFAULT_TEST_LIBRARY_NAME);
        final String actualCodeRequestUrlWithLoginHint = requestWithLoginHint.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Matching login hint", actualCodeRequestUrlWithLoginHint.contains("login_hint="+DEFAULT_TEST_LOGIN_HINT));
        assertTrue("Matching response type", actualCodeRequestUrlWithLoginHint.contains("response_type=code"));
        assertTrue("Matching correlation id", actualCodeRequestUrlWithLoginHint.contains("&client-request-id=" + DEFAULT_TEST_CORRELATION_ID.toString()));
        assertTrue("Matching library version", actualCodeRequestUrlWithLoginHint.contains("&x-client-Ver="+DEFAULT_TEST_VERSION));

    }

    @Test
    public void testGetCodeRequestUrlWithForceLoginPrompt() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, null, DEFAULT_TEST_VERSION,
                MicrosoftStsAuthorizationRequest.Prompt.FORCE_LOGIN, null, null, null, null, DEFAULT_TEST_LIBRARY_NAME);
        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Prompt", actualCodeRequestUrl.contains("&prompt="+MicrosoftStsAuthorizationRequest.Prompt.FORCE_LOGIN));
    }

    @Test
    public void testGetCodeRequestUrlWithSelectAccountPrompt() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, null, DEFAULT_TEST_VERSION,
                MicrosoftStsAuthorizationRequest.Prompt.SELECT_ACCOUNT, null, null, null, null, DEFAULT_TEST_LIBRARY_NAME);
        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Prompt", actualCodeRequestUrl.contains("&prompt="+MicrosoftStsAuthorizationRequest.Prompt.SELECT_ACCOUNT));
        assertTrue("Matching message",
                actualCodeRequestUrl.contains("&x-client-SKU="+DEFAULT_TEST_LIBRARY_NAME));
        assertTrue("Matching message",
                actualCodeRequestUrl.contains("&x-client-Ver="+DEFAULT_TEST_VERSION));
    }

    @Test
    public void testGetCodeRequestUrlWithExtraQP() throws MalformedURLException, UnsupportedEncodingException, ClientException {
        final MicrosoftStsAuthorizationRequest request = createAuthenticationRequest(DEFAULT_TEST_RESPONSETYPE,
                DEFAULT_TEST_CLIENT_ID, DEFAULT_TEST_REDIRECT_URI, null, DEFAULT_TEST_SCOPE,
                getValidRequestUrl(), DEFAULT_TEST_LOGIN_HINT,
                DEFAULT_TEST_CORRELATION_ID, null, DEFAULT_TEST_EXTRA_QP, DEFAULT_TEST_VERSION,
                DEFAULT_TEST_PROMPT, null, null, null, DEFAULT_TEST_SLICE_PARAMETER, DEFAULT_TEST_LIBRARY_NAME);
        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
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
                    getValidRequestUrl(), DEFAULT_TEST_LOGIN_HINT,
                    DEFAULT_TEST_CORRELATION_ID, null, DEFAULT_TEST_EXTRA_QP, DEFAULT_TEST_VERSION,
                    DEFAULT_TEST_PROMPT, null, null, null, DEFAULT_TEST_SLICE_PARAMETER, DEFAULT_TEST_LIBRARY_NAME);
        } catch (final Exception exception) {
            assertTrue(exception instanceof IllegalArgumentException);
            assertTrue(exception.getMessage().contains("clientId is empty"));
        }
    }
}
