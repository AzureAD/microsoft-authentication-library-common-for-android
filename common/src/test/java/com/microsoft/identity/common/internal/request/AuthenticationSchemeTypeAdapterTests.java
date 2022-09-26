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
package com.microsoft.identity.common.internal.request;

import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.net.MalformedURLException;
import java.net.URL;

@RunWith(RobolectricTestRunner.class)
public class AuthenticationSchemeTypeAdapterTests {

    @Test
    public void testSerialize_BearerAuthenticationSchemeInternal() {
        final String expectedJson = "{\"name\":\"Bearer\"}";
        final BearerAuthenticationSchemeInternal bearerAuthenticationSchemeInternal = new BearerAuthenticationSchemeInternal();

        final String json = AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(bearerAuthenticationSchemeInternal);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testSerialize_PopAuthenticationSchemeInternal() throws MalformedURLException {
        final String expectedJson =
                "{\"http_method\":\"GET\",\"url\":\"https://xyz.com\",\"nonce\":\"nonce_test\",\"client_claims\":\"clientClaims_test\",\"name\":\"PoP\"}";
        final PopAuthenticationSchemeInternal popAuthenticationSchemeInternal =
                new PopAuthenticationSchemeInternal(
                        Mockito.mock(IDevicePopManager.class),
                        "GET",
                        new URL("https://xyz.com"),
                        "nonce_test",
                        "clientClaims_test");

        final String json = AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(popAuthenticationSchemeInternal);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testSerialize_PopAuthenticationSchemeWithClientKeyInternal() throws MalformedURLException {
        final String expectedJson =
                "{\"http_method\":\"GET\",\"url\":\"https://xyz.com\",\"nonce\":\"nonce_test\",\"client_claims\":\"clientClaims_test\",\"kid\":\"kid_test\",\"name\":\"PoP_With_Client_Key\"}";
        final PopAuthenticationSchemeWithClientKeyInternal popAuthenticationSchemeWithClientKeyInternal =
                new PopAuthenticationSchemeWithClientKeyInternal(
                        "GET",
                        new URL("https://xyz.com"),
                        "nonce_test",
                        "clientClaims_test",
                        "kid_test");

        final String json = AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(popAuthenticationSchemeWithClientKeyInternal);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testDeserialize_BearerAuthenticationSchemeInternal() {
        final String json = "{\"name\":\"Bearer\"}";
        final AbstractAuthenticationScheme authenticationScheme =
                AuthenticationSchemeTypeAdapter.getGsonInstance().fromJson(json, AbstractAuthenticationScheme.class);
        Assert.assertTrue(authenticationScheme instanceof BearerAuthenticationSchemeInternal);
    }

    @Test
    public void testDeserialize_PopAuthenticationSchemeInternal() {
        final String json =
                "{\"http_method\":\"GET\",\"url\":\"https://xyz.com\",\"nonce\":\"nonce_test\",\"client_claims\":\"clientClaims_test\",\"name\":\"PoP\"}";
        final AbstractAuthenticationScheme authenticationScheme =
                AuthenticationSchemeTypeAdapter.getGsonInstance().fromJson(json, AbstractAuthenticationScheme.class);

        Assert.assertTrue(authenticationScheme instanceof PopAuthenticationSchemeInternal);
    }

    @Test
    public void testDeserialize_PopAuthenticationSchemeWithClientKeyInternal() {
        final String json =
                "{\"http_method\":\"GET\",\"url\":\"https://xyz.com\",\"nonce\":\"nonce_test\",\"client_claims\":\"clientClaims_test\",\"kid\":\"kid_test\",\"name\":\"PoP_With_Client_Key\"}";
        final AbstractAuthenticationScheme authenticationScheme =
                AuthenticationSchemeTypeAdapter.getGsonInstance().fromJson(json, AbstractAuthenticationScheme.class);

        Assert.assertTrue(authenticationScheme instanceof PopAuthenticationSchemeWithClientKeyInternal);
    }
}
