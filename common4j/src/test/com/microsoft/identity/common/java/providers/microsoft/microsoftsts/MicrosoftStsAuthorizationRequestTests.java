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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.java.TestUtils;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.platform.MockDeviceMetadata;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.identity.common.java.providers.Constants.MOCK_PKCE_CHALLENGE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE_ENCODED;
import static com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest.HIDE_SWITCH_USER_QUERY_PARAMETER;
import static org.junit.Assert.assertTrue;

public class MicrosoftStsAuthorizationRequestTests {
    private static final String DEFAULT_TEST_CLIENT_ID = "some-client-id";
    private static final String DEFAULT_TEST_REDIRECT_URI = "some://redirect.uri";
    private static final String DEFAULT_TEST_REDIRECT_URI_ENCODED = "some%3A%2F%2Fredirect.uri";
    private static final String DEFAULT_TEST_LOGIN_HINT = "someLoginHint";
    private static final String DEFAULT_TEST_SCOPE = "scope1 scope2";
    private static final String DEFAULT_TEST_SCOPE_ENCODED = "scope1+scope2";
    private static final String DEFAULT_TEST_AUTHORIZATION_ENDPOINT = "https://login.microsoftonline.com/common/oAuth2/v2.0/authorize";
    private static final UUID DEFAULT_TEST_CORRELATION_ID = UUID.randomUUID();
    private static final List<Map.Entry<String, String>> DEFAULT_TEST_EXTRA_QP = new ArrayList<Map.Entry<String, String>>() {{
        add(new AbstractMap.SimpleEntry<>("extra", "1"));
        add(new AbstractMap.SimpleEntry<>("haschrome", "1"));
    }};

    public static final String MOCK_FLIGHT_QUERY_1 = "MOCK_FLIGHT_QUERY_1";
    public static final String MOCK_FLIGHT_QUERY_2 = "MOCK_FLIGHT_QUERY_2";
    public static final String MOCK_FLIGHT_VALUE_1 = "MOCK_FLIGHT_VALUE_1";
    public static final String MOCK_FLIGHT_VALUE_2 = "MOCK_FLIGHT_VALUE_2";
    private static final Map<String, String> DEFAULT_FLIGHT_PARAMETER = new LinkedHashMap<String, String>() {{
        put(MOCK_FLIGHT_QUERY_1, MOCK_FLIGHT_VALUE_1);
        put(MOCK_FLIGHT_QUERY_2, MOCK_FLIGHT_VALUE_2);
    }};


    final AzureActiveDirectorySlice DEFAULT_TEST_SLICE = new AzureActiveDirectorySlice(
            DEFAULT_TEST_SLICE_PARAMETER,
            DEFAULT_TEST_DATA_CENTER
    );

    private static final String DEFAULT_TEST_PROMPT = MicrosoftStsAuthorizationRequest.Prompt.CONSENT;
    private static final String DEFAULT_TEST_UID = "1";
    private static final String DEFAULT_TEST_UTID = "1234-5678-90abcdefg";
    private static final String DEFAULT_TEST_DISPLAYABLEID = "user@contoso.com";
    private static final String DEFAULT_TEST_SLICE_PARAMETER = "testSlice";
    private static final String DEFAULT_TEST_DATA_CENTER = "prod-wst-test1";
    private static final String DEFAULT_TEST_AUTHORITY_STRING = "https://login.microsoftonline.com/common";

    private static final String TEST_CP_VERSION = "5.5.5555";

    private static final String CONSTANT_LOGIN_HINT = "login_hint";
    private static final String CONSTANT_INSTANCE_AWARE = "instance_aware";

    @After
    public void tearDown() {
        Device.clearDeviceMetadata();
    }

    static URL getValidRequestUrl() throws MalformedURLException {
        return new URL(DEFAULT_TEST_AUTHORITY_STRING);
    }

    // Check that we're not sending anything unexpected to the server side
    // by comparing the resulted URL by-character.
    @Test
    public void testCreateUriFromAuthorizationRequest() throws MalformedURLException, URISyntaxException, ClientException {
        Device.setDeviceMetadata(new MockDeviceMetadata());

        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setPrompt(DEFAULT_TEST_PROMPT)
                .setUid(DEFAULT_TEST_UID)
                .setUtid(DEFAULT_TEST_UTID)
                .setInstalledCompanyPortalVersion(TEST_CP_VERSION)
                .setSlice(DEFAULT_TEST_SLICE)
                .setFlightParameters(DEFAULT_FLIGHT_PARAMETER)
                .setDisplayableId(DEFAULT_TEST_DISPLAYABLEID)

                // Values from base class.
                .setCorrelationId(DEFAULT_TEST_CORRELATION_ID)
                .setPkceChallenge(MOCK_PKCE_CHALLENGE)
                .setAuthority(getValidRequestUrl())

                .setClientId(DEFAULT_TEST_CLIENT_ID)
                .setRedirectUri(DEFAULT_TEST_REDIRECT_URI)
                .setState(MOCK_STATE)
                .setScope(DEFAULT_TEST_SCOPE)
                .build();

        Assert.assertEquals(DEFAULT_TEST_AUTHORIZATION_ENDPOINT +
                        "?prompt=" + DEFAULT_TEST_PROMPT +
                        "&login_req=" + DEFAULT_TEST_UID +
                        "&domain_req=" + DEFAULT_TEST_UTID +
                        "&cpVersion=" + TEST_CP_VERSION +
                        // Base class fields start here.
                        "&client-request-id=" + DEFAULT_TEST_CORRELATION_ID +
                        "&code_challenge=" + MOCK_PKCE_CHALLENGE.getCodeChallenge() +
                        "&code_challenge_method=" + MOCK_PKCE_CHALLENGE.getCodeChallengeMethod() +
                        "&x-client-OS=" + MockDeviceMetadata.TEST_OS_ESTS +
                        "&x-client-CPU=" + MockDeviceMetadata.TEST_CPU +
                        "&x-client-DM=" + MockDeviceMetadata.TEST_DEVICE_MODEL +
                        "&response_type=code" +
                        "&client_id=" + DEFAULT_TEST_CLIENT_ID +
                        "&redirect_uri=" + DEFAULT_TEST_REDIRECT_URI_ENCODED +
                        "&state=" + MOCK_STATE_ENCODED +
                        "&scope=" + DEFAULT_TEST_SCOPE_ENCODED +
                        "&" + MOCK_FLIGHT_QUERY_1 + "=" + MOCK_FLIGHT_VALUE_1 +
                        "&" + MOCK_FLIGHT_QUERY_2 + "=" + MOCK_FLIGHT_VALUE_2 +
                        "&slice=" + DEFAULT_TEST_SLICE_PARAMETER +
                        "&dc=" + DEFAULT_TEST_DATA_CENTER,
                request.getAuthorizationRequestAsHttpRequest().toString());

        Assert.assertEquals(DEFAULT_TEST_DISPLAYABLEID, request.getDisplayableId());
    }

    @Test(expected = IllegalStateException.class)
    public void testGenerateUrlWithoutAuthority() throws ClientException {
        final MicrosoftStsAuthorizationRequest requestWithLoginHint = new MicrosoftStsAuthorizationRequest.Builder().build();
        requestWithLoginHint.getAuthorizationRequestAsHttpRequest();
    }


    @Test
    public void testGetCodeRequestUrlWithLoginHint() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest requestWithLoginHint = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setLoginHint(DEFAULT_TEST_LOGIN_HINT)
                .build();

        final String actualCodeRequestUrlWithLoginHint = requestWithLoginHint.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Matching login hint", actualCodeRequestUrlWithLoginHint.contains("login_hint=" + DEFAULT_TEST_LOGIN_HINT));
        assertTrue("Matching HideSwitchUser", actualCodeRequestUrlWithLoginHint.contains("hsu=1"));
    }

    @Test
    public void testGetCodeRequestWithDuplicatedExtraQueryParametersHsu() throws MalformedURLException, ClientException {
        final int expectedCount = 1;
        final MicrosoftStsAuthorizationRequest.Builder requestWithLoginHint = new MicrosoftStsAuthorizationRequest.Builder()
                .setLoginHint(DEFAULT_TEST_LOGIN_HINT)
                .setAuthority(getValidRequestUrl());
        final List<Map.Entry<String, String>> extraQueryParameter = new LinkedList<>();
        extraQueryParameter.add(new AbstractMap.SimpleEntry<>(HIDE_SWITCH_USER_QUERY_PARAMETER, "1"));
        requestWithLoginHint.setExtraQueryParams(extraQueryParameter);
        final String actualCodeRequestUrl = requestWithLoginHint.build().getAuthorizationRequestAsHttpRequest().toString();

        Assert.assertEquals(expectedCount, TestUtils.countMatches(actualCodeRequestUrl, HIDE_SWITCH_USER_QUERY_PARAMETER));
    }

    @Test
    public void testGetCodeRequestWithDuplicatedExtraQueryParametersLoginHint() throws MalformedURLException, ClientException {
        final int expectedCount = 1;
        final MicrosoftStsAuthorizationRequest.Builder requestWithLoginHint = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl());
        final List<Map.Entry<String, String>> extraQueryParameter = new LinkedList<>();
        extraQueryParameter.add(new AbstractMap.SimpleEntry<>(CONSTANT_LOGIN_HINT, DEFAULT_TEST_LOGIN_HINT));
        requestWithLoginHint.setExtraQueryParams(extraQueryParameter);
        final String actualCodeRequestUrlWithLoginHint = requestWithLoginHint.build().getAuthorizationRequestAsHttpRequest().toString();

        Assert.assertTrue(actualCodeRequestUrlWithLoginHint.contains(CONSTANT_LOGIN_HINT));
        Assert.assertEquals(expectedCount, TestUtils.countMatches(actualCodeRequestUrlWithLoginHint, "login_hint"));
    }

    @Test
    public void testGetCodeRequestWithDuplicatedExtraQueryParametersInstanceAware() throws MalformedURLException, ClientException {
        final int expectedCount = 1;
        final MicrosoftStsAuthorizationRequest.Builder requestWithLoginHint = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl());
        final List<Map.Entry<String, String>> extraQueryParameter = new LinkedList<>();
        extraQueryParameter.add(new AbstractMap.SimpleEntry<>(CONSTANT_LOGIN_HINT, DEFAULT_TEST_LOGIN_HINT));
        extraQueryParameter.add(new AbstractMap.SimpleEntry<>(CONSTANT_INSTANCE_AWARE, Boolean.TRUE.toString()));
        requestWithLoginHint.setExtraQueryParams(extraQueryParameter);
        final String actualCodeRequestUrl = requestWithLoginHint.build().getAuthorizationRequestAsHttpRequest().toString();

        Assert.assertTrue(actualCodeRequestUrl.contains(CONSTANT_LOGIN_HINT));
        Assert.assertTrue(actualCodeRequestUrl.contains(CONSTANT_INSTANCE_AWARE));
        Assert.assertEquals(expectedCount, TestUtils.countMatches(actualCodeRequestUrl, CONSTANT_LOGIN_HINT));
        Assert.assertEquals(expectedCount, TestUtils.countMatches(actualCodeRequestUrl, CONSTANT_INSTANCE_AWARE));
    }

    @Test
    public void testGetCodeRequestUrlWithForceLoginPrompt() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setPrompt(MicrosoftStsAuthorizationRequest.Prompt.FORCE_LOGIN)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Prompt", actualCodeRequestUrl.contains("prompt=" + MicrosoftStsAuthorizationRequest.Prompt.FORCE_LOGIN));
    }

    @Test
    public void testGetCodeRequestUrlWithSelectAccountPrompt() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setPrompt(MicrosoftStsAuthorizationRequest.Prompt.SELECT_ACCOUNT)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Prompt", actualCodeRequestUrl.contains("prompt=" + MicrosoftStsAuthorizationRequest.Prompt.SELECT_ACCOUNT));
    }

    @Test
    public void testGetCodeRequestUrlWithExtraQP() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setPrompt(DEFAULT_TEST_PROMPT)
                .setExtraQueryParams(DEFAULT_TEST_EXTRA_QP)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Extra parameters 1", actualCodeRequestUrl.contains("extra=1"));
        assertTrue("Extra parameters 2", actualCodeRequestUrl.contains("haschrome=1"));
        assertTrue("Prompt", actualCodeRequestUrl.contains("prompt=consent"));
    }

    @Test
    public void testGetCodeRequestUrlWithResponseType() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .build();
        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Response type", actualCodeRequestUrl.contains("response_type=" + AuthorizationRequest.ResponseType.CODE));
    }

    @Test
    public void testRequestWithSliceParameter() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setSlice(DEFAULT_TEST_SLICE)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Slice", actualCodeRequestUrl.contains("slice=" + DEFAULT_TEST_SLICE_PARAMETER));
        assertTrue("DC", actualCodeRequestUrl.contains("dc=" + DEFAULT_TEST_DATA_CENTER));
    }

    @Test
    public void testRequestWithCpVersion() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setInstalledCompanyPortalVersion(TEST_CP_VERSION)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("CP Version", actualCodeRequestUrl.contains("cpVersion=" + TEST_CP_VERSION));
    }

    @Test
    public void testSetFlightParameters() throws MalformedURLException, ClientException {
        final MicrosoftStsAuthorizationRequest request = new MicrosoftStsAuthorizationRequest.Builder()
                .setAuthority(getValidRequestUrl())
                .setFlightParameters(DEFAULT_FLIGHT_PARAMETER)
                .build();

        final String actualCodeRequestUrl = request.getAuthorizationRequestAsHttpRequest().toString();
        assertTrue("Flight Param 1", actualCodeRequestUrl.contains(MOCK_FLIGHT_QUERY_1 + "=" + MOCK_FLIGHT_VALUE_1));
        assertTrue("Flight Param 2", actualCodeRequestUrl.contains(MOCK_FLIGHT_QUERY_2 + "=" + MOCK_FLIGHT_VALUE_2));
    }
}
