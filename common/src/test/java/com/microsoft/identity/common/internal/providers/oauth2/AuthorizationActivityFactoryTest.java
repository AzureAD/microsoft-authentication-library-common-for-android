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
package com.microsoft.identity.common.internal.providers.oauth2;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleCredential;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;

import lombok.SneakyThrows;

/**
 * Tests for @link{AuthorizationActivityFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public class AuthorizationActivityFactoryTest {

    @SneakyThrows
    @Test
    public void testGetAuthorizationActivityIntent() {
        // Arrange
        final Context context = RuntimeEnvironment.getApplication();
        final Intent authIntent = new Intent();
        final String requestUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=123&response_type=code&redirect_uri=msauth%3A%2F%2Fexample.com%2Fredirect";
        final String redirectUri = "msauth://example.com/redirect";
        final HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("header1", "value1");
        final AuthorizationAgent authorizationAgent = AuthorizationAgent.WEBVIEW;
        final boolean webViewZoomEnabled = true;
        final boolean webViewZoomControlsEnabled = true;
        final String sourceLibraryName = "TestLibrary";
        final String sourceLibraryVersion = "1.0.0";
        final String idToken = "idToken";
        final SignInWithGoogleCredential signInWithGoogleCredential = new SignInWithGoogleCredential(idToken);

        final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(
                context,
                authIntent,
                requestUrl,
                redirectUri,
                requestHeaders,
                authorizationAgent,
                webViewZoomEnabled,
                webViewZoomControlsEnabled,
                sourceLibraryName,
                sourceLibraryVersion,
                signInWithGoogleCredential
        );

        assertEquals(AuthorizationActivity.class.getName(), resultIntent.getComponent().getClassName());
        assertEquals(authIntent, resultIntent.getParcelableExtra(AUTH_INTENT));
        assertEquals(redirectUri, resultIntent.getStringExtra(REDIRECT_URI));
        assertEquals(authorizationAgent, resultIntent.getSerializableExtra(AUTHORIZATION_AGENT));
        assertEquals(webViewZoomEnabled, resultIntent.getBooleanExtra(WEB_VIEW_ZOOM_ENABLED, false));
        assertEquals(webViewZoomControlsEnabled, resultIntent.getBooleanExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, false));
        assertEquals(sourceLibraryName, resultIntent.getStringExtra(PRODUCT));
        assertEquals(sourceLibraryVersion, resultIntent.getStringExtra(VERSION));

        final String receivedUrl = resultIntent.getStringExtra(REQUEST_URL);
        final String expectedUrl = requestUrl + "&id_provider=google.com";
        assertEquals(expectedUrl, receivedUrl);

        final HashMap<String, String> receivedHeaders = (HashMap<String, String>) resultIntent.getSerializableExtra(REQUEST_HEADERS);
        final String idTokenHeaderValue = receivedHeaders.get("x-ms-fidp-idtoken");
        assertNotNull(idTokenHeaderValue);
        assertEquals(idToken, idTokenHeaderValue);
    }
}
