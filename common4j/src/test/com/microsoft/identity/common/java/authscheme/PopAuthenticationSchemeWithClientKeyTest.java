//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.authscheme;

import com.microsoft.identity.common.java.exception.ClientException;
import com.nimbusds.jose.util.Base64URL;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class PopAuthenticationSchemeWithClientKeyTest {
    private final String httpMethod = "GET";
    private final String nonce = "123456";
    private final String url = "http://foo.com";
    private final String kid = "xyz";
    private final String clientClaims = "claim1_key:claim1_Value";

    private final PopAuthenticationSchemeWithClientKeyInternal authenticationSchemeWithClientKeyInternal;

    public PopAuthenticationSchemeWithClientKeyTest() throws MalformedURLException {
        authenticationSchemeWithClientKeyInternal = PopAuthenticationSchemeWithClientKeyInternal.builder()
                .httpMethod(httpMethod)
                .nonce(nonce)
                .url(new URL(url))
                .kid(kid)
                .clientClaims(clientClaims)
                .build();
    }

    @Test
    public void testBuilder() {
        Assert.assertEquals(httpMethod, authenticationSchemeWithClientKeyInternal.getHttpMethod());
        Assert.assertEquals(clientClaims, authenticationSchemeWithClientKeyInternal.getClientClaims());
        Assert.assertEquals(kid, authenticationSchemeWithClientKeyInternal.getKid());
        Assert.assertEquals(nonce, authenticationSchemeWithClientKeyInternal.getNonce());
        Assert.assertEquals(url, authenticationSchemeWithClientKeyInternal.getUrl().toString());
    }

    @Test
    public void testGetRequestConfirmation() {
        final String expectedReqCnfJson = Base64URL.encode(new JSONObject().put("kid", kid).toString()).toString();
        Assert.assertEquals(expectedReqCnfJson, authenticationSchemeWithClientKeyInternal.getRequestConfirmation());
    }

    @Test
    public void testGetAccessToken() throws ClientException {
        final String accessToken = "accessToken-1234";
        Assert.assertEquals(accessToken, authenticationSchemeWithClientKeyInternal.getAccessTokenForScheme(accessToken));
    }
}
