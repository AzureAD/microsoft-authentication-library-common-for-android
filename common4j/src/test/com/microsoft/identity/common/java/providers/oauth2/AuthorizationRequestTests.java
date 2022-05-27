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
package com.microsoft.identity.common.java.providers.oauth2;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.java.providers.Constants.MOCK_CLAIMS;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_CLAIMS_ENCODED;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_CLIENT_ID;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_EXTRA_QUERY_PARAMS;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_HEADER_1;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_HEADER_2;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_QUERY_1;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_QUERY_2;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_REQUEST_HEADERS;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_RESPONSE_TYPE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_SCOPE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_SCOPE_ENCODED;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE_ENCODED;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_VALUE_1;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_VALUE_2;

@RunWith(JUnit4.class)
public class AuthorizationRequestTests {
    @Test
    public void testCreateUriFromEmptyAuthorizationRequest() throws ClientException {
        final MockAuthorizationRequest.Builder builder = new MockAuthorizationRequest.Builder();
        Assert.assertEquals(MockAuthorizationRequest.MOCK_AUTH_ENDPOINT + "?response_type=code",
                builder.build().getAuthorizationRequestAsHttpRequest().toString());
    }

    // Check that we're not sending anything unexpected to the server side
    // by comparing the resulted URL by-character.
    @Test
    public void testCreateUriFromAuthorizationRequest() throws ClientException {
        final MockAuthorizationRequest request = new MockAuthorizationRequest.Builder()
                .setClientId(MOCK_CLIENT_ID)
                .setClaims(MOCK_CLAIMS)
                .setExtraQueryParams(MOCK_EXTRA_QUERY_PARAMS)
                .setScope(MOCK_SCOPE)
                .setRedirectUri(MOCK_REDIRECT_URI)
                .setRequestHeaders(MOCK_REQUEST_HEADERS)            // Shouldn't be in the URI.
                .setResponseType(MOCK_RESPONSE_TYPE)
                .setState(MOCK_STATE)
                .setWebViewZoomControlsEnabled(true)
                .setWebViewZoomEnabled(true)
                .build();

        Assert.assertEquals(MockAuthorizationRequest.MOCK_AUTH_ENDPOINT +
                        "?response_type=" + MOCK_RESPONSE_TYPE +
                        "&client_id=" + MOCK_CLIENT_ID +
                        "&redirect_uri=" + MOCK_REDIRECT_URI +
                        "&state=" + MOCK_STATE_ENCODED +
                        "&scope=" + MOCK_SCOPE_ENCODED +
                        "&claims=" + MOCK_CLAIMS_ENCODED +
                        "&" + MOCK_QUERY_1 + "=" + MOCK_VALUE_1 +
                        "&" + MOCK_QUERY_2 + "=" + MOCK_VALUE_2
                ,
                request.getAuthorizationRequestAsHttpRequest().toString());

        Assert.assertEquals(2, request.getRequestHeaders().size());
        Assert.assertEquals(MOCK_VALUE_1, request.getRequestHeaders().get(MOCK_HEADER_1));
        Assert.assertEquals(MOCK_VALUE_2, request.getRequestHeaders().get(MOCK_HEADER_2));
    }
}
