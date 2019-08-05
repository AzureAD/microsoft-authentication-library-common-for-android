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

import android.util.Pair;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private static final List<Pair<String, String>> DEFAULT_TEST_EXTRA_QP = new ArrayList<Pair<String, String>>() {{
        add(new Pair<>("extra", "1"));
        add(new Pair<>("haschrome", "1"));
    }};
    private static final String DEFAULT_TEST_PROMPT = MicrosoftStsAuthorizationRequest.Prompt.CONSENT;
    private static final String DEFAULT_TEST_UID = "1";
    private static final String DEFAULT_TEST_UTID = "1234-5678-90abcdefg";
    private static final String DEFAULT_TEST_DISPLAYABLEID = "user@contoso.com";
    private static final String DEFAULT_TEST_SLICE_PARAMETER = "slice=myslice";
    private static final String DEFAULT_TEST_AUTHORITY_STRING = "https://login.microsoftonline.com/common";

    private static final String CONSTANT_LOGIN_HINT = "login_hint";
    private static final String CONSTANT_INSTANCE_AWARE = "instance_aware";

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(DEFAULT_TEST_AUTHORITY_STRING);
    }

    static MicrosoftStsAuthorizationRequest.Builder getBaseBuilder() throws MalformedURLException {
        return new MicrosoftStsAuthorizationRequest.Builder()
                .setResponseType(DEFAULT_TEST_RESPONSETYPE)
                .setRedirectUri(DEFAULT_TEST_REDIRECT_URI)
                .setClientId(DEFAULT_TEST_CLIENT_ID)
                .setAuthority(getValidRequestUrl())
                .setScope(DEFAULT_TEST_SCOPE)
                .setLoginHint(DEFAULT_TEST_LOGIN_HINT)
                .setCorrelationId(DEFAULT_TEST_CORRELATION_ID);
    }


    @Test
    public void testGetCodeRequestUrlWithLoginHint() throws MalformedURLException, UnsupportedEncodingException {

        final MicrosoftStsAuthorizationRequest requestWithLoginHint = getBaseBuilder().build();

        final String actualCodeRequestUrlWithLoginHint = requestWithLoginHint.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Matching login hint", actualCodeRequestUrlWithLoginHint.contains("login_hint=" + DEFAULT_TEST_LOGIN_HINT));
        assertTrue("Matching response type", actualCodeRequestUrlWithLoginHint.contains("response_type=code"));
        assertTrue("Matching correlation id", actualCodeRequestUrlWithLoginHint.contains("client-request-id=" + DEFAULT_TEST_CORRELATION_ID.toString()));

    }

    @Test
    public void testGetCodeRequestWithDuplicatedExtraQueryParametersLoginHint() throws MalformedURLException {
        final int expectedCount = 1;
        final MicrosoftStsAuthorizationRequest.Builder requestWithLoginHint = getBaseBuilder();
        final List<Pair<String, String>> extraQueryParameter = new LinkedList<>();
        extraQueryParameter.add(new Pair<>(CONSTANT_LOGIN_HINT, DEFAULT_TEST_LOGIN_HINT));
        requestWithLoginHint.setExtraQueryParams(extraQueryParameter);
        final String actualCodeRequestUrlWithLoginHint = requestWithLoginHint.build().getAuthorizationRequestAsHttpRequest().toString();

        Assert.assertTrue(actualCodeRequestUrlWithLoginHint.contains(CONSTANT_LOGIN_HINT));
        Assert.assertTrue(expectedCount == StringUtil.countMatches(actualCodeRequestUrlWithLoginHint, "login_hint"));
    }

    @Test
    public void testGetCodeRequestWithDuplicatedExtraQueryParametersInstanceAware() throws MalformedURLException {
        final int expectedCount = 1;
        final MicrosoftStsAuthorizationRequest.Builder requestWithLoginHint = getBaseBuilder();
        final List<Pair<String, String>> extraQueryParameter = new LinkedList<>();
        extraQueryParameter.add(new Pair<>(CONSTANT_LOGIN_HINT, DEFAULT_TEST_LOGIN_HINT));
        extraQueryParameter.add(new Pair<>(CONSTANT_INSTANCE_AWARE, Boolean.TRUE.toString()));
        requestWithLoginHint.setExtraQueryParams(extraQueryParameter);
        final String actualCodeRequestUrl = requestWithLoginHint.build().getAuthorizationRequestAsHttpRequest().toString();

        Assert.assertTrue(actualCodeRequestUrl.contains(CONSTANT_LOGIN_HINT));
        Assert.assertTrue(actualCodeRequestUrl.contains(CONSTANT_INSTANCE_AWARE));
        Assert.assertTrue(expectedCount == StringUtil.countMatches(actualCodeRequestUrl, CONSTANT_LOGIN_HINT));
        Assert.assertTrue(expectedCount == StringUtil.countMatches(actualCodeRequestUrl, CONSTANT_INSTANCE_AWARE));
    }

    @Test
    public void testGetCodeRequestUrlWithForceLoginPrompt() throws MalformedURLException, UnsupportedEncodingException {

        final MicrosoftStsAuthorizationRequest request = getBaseBuilder()
                .setPrompt(MicrosoftStsAuthorizationRequest.Prompt.FORCE_LOGIN)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Prompt", actualCodeRequestUrl.contains("prompt=" + MicrosoftStsAuthorizationRequest.Prompt.FORCE_LOGIN));
    }

    @Test
    public void testGetCodeRequestUrlWithSelectAccountPrompt() throws MalformedURLException, UnsupportedEncodingException {

        final MicrosoftStsAuthorizationRequest request = getBaseBuilder()
                .setPrompt(MicrosoftStsAuthorizationRequest.Prompt.SELECT_ACCOUNT)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Prompt", actualCodeRequestUrl.contains("prompt=" + MicrosoftStsAuthorizationRequest.Prompt.SELECT_ACCOUNT));
    }

    @Test
    public void testGetCodeRequestUrlWithExtraQP() throws MalformedURLException, UnsupportedEncodingException {

        final MicrosoftStsAuthorizationRequest request = getBaseBuilder()
                .setPrompt(DEFAULT_TEST_PROMPT)
                .setExtraQueryParams(DEFAULT_TEST_EXTRA_QP)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Extra parameters 1", actualCodeRequestUrl.contains("extra=1"));
        assertTrue("Extra parameters 2", actualCodeRequestUrl.contains("haschrome=1"));
        assertTrue("Prompt", actualCodeRequestUrl.contains("prompt=consent"));
    }


    @Test
    public void testGetCodeRequestUrlWithResponseType() throws MalformedURLException, UnsupportedEncodingException {
        final MicrosoftStsAuthorizationRequest request = getBaseBuilder().build();
        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Response type", actualCodeRequestUrl.contains("response_type=" + AuthorizationRequest.ResponseType.CODE));
    }
}
