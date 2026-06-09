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
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;

import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleApi;
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleCredential;
import com.microsoft.identity.common.internal.msafederation.google.SignInWithGoogleParameters;
import com.microsoft.identity.common.internal.ui.browser.AndroidBrowserSelector;
import com.microsoft.identity.common.java.browser.Browser;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;
import java.util.HashMap;

import lombok.SneakyThrows;

/**
 * Tests for {@link AuthorizationActivityFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public class AuthorizationActivityFactoryTest {

    private final Context context = RuntimeEnvironment.getApplication();
    private final Intent authIntent = new Intent();
    private final String requestUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=123&response_type=code&redirect_uri=msauth%3A%2F%2Fexample.com%2Fredirect";
    private final String redirectUri = "msauth://example.com/redirect";
    private final HashMap<String, String> requestHeaders = new HashMap<>();
    {
        requestHeaders.put("header1", "value1");
    }
    private final AuthorizationAgent authorizationAgent = AuthorizationAgent.WEBVIEW;
    private final boolean webViewZoomEnabled = true;
    private final boolean webViewZoomControlsEnabled = true;
    private final String sourceLibraryName = "TestLibrary";
    private final String sourceLibraryVersion = "1.0.0";
    final String idToken = "idToken";
    private final AuthorizationActivityParameters authorizationActivityParameters = new AuthorizationActivityParameters(
            context,
            authIntent,
            requestUrl,
            redirectUri,
            requestHeaders,
            authorizationAgent,
            webViewZoomEnabled,
            webViewZoomControlsEnabled,
            sourceLibraryName,
            sourceLibraryVersion
    );

    @SneakyThrows
    @Test
    public void testGetAuthorizationActivityIntent() {
        final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(
                authorizationActivityParameters
        );
        assertEquals(AuthorizationActivity.class.getName(), resultIntent.getComponent().getClassName());
        assertEquals(authIntent, resultIntent.getParcelableExtra(AUTH_INTENT));
        assertEquals(redirectUri, resultIntent.getStringExtra(REDIRECT_URI));
        assertEquals(authorizationAgent, resultIntent.getSerializableExtra(AUTHORIZATION_AGENT));
        assertEquals(webViewZoomEnabled, resultIntent.getBooleanExtra(WEB_VIEW_ZOOM_ENABLED, false));
        assertEquals(webViewZoomControlsEnabled, resultIntent.getBooleanExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, false));
        assertEquals(sourceLibraryName, resultIntent.getStringExtra(PRODUCT));
        assertEquals(sourceLibraryVersion, resultIntent.getStringExtra(VERSION));
        assertEquals(requestUrl,  resultIntent.getStringExtra(REQUEST_URL));

        final HashMap<String, String> receivedHeaders = (HashMap<String, String>) resultIntent.getSerializableExtra(REQUEST_HEADERS);
        assertNotNull(receivedHeaders);
        final String idTokenHeaderValue = receivedHeaders.get("header1");
        assertNotNull(idTokenHeaderValue);
        assertEquals("value1", idTokenHeaderValue);
        assertFalse(resultIntent.hasExtra(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT));
    }

    @SneakyThrows
    @Test
    public void testSignInWithGoogleAndGetAuthorizationActivityIntent() {
        // Arrange
        final Activity mockActivity = Robolectric.buildActivity(Activity.class).get();
        // mock SignInWithGoogleApi using mockito
        final SignInWithGoogleApi mockSignInWithGoogleApi = mock(SignInWithGoogleApi.class);
        final SignInWithGoogleCredential mockCredential = new SignInWithGoogleCredential(idToken);
        when(mockSignInWithGoogleApi.signInSync(any(SignInWithGoogleParameters.class))).thenReturn(mockCredential);

        SignInWithGoogleApi.setInstance(mockSignInWithGoogleApi);
        final SignInWithGoogleParameters siwgParams = new SignInWithGoogleParameters(mockActivity);
        final Intent resultIntent = AuthorizationActivityFactory.signInWithGoogleAndGetAuthorizationActivityIntent(
                authorizationActivityParameters,
                siwgParams
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
        assertNotNull(receivedHeaders);
        final String idTokenHeaderValue = receivedHeaders.get("x-ms-fidp-idtoken");
        assertNotNull(idTokenHeaderValue);
        assertEquals(idToken, idTokenHeaderValue);
    }

    @SneakyThrows
    @Test
    public void testGetAuthorizationActivityIntentWithGoogleCredential() {
        // Arrange
        final SignInWithGoogleCredential signInWithGoogleCredential = new SignInWithGoogleCredential(idToken);
        final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(
                authorizationActivityParameters,
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
        assertNotNull(receivedHeaders);
        final String idTokenHeaderValue = receivedHeaders.get("x-ms-fidp-idtoken");
        assertNotNull(idTokenHeaderValue);
        assertEquals(idToken, idTokenHeaderValue);
    }

    @Test
    public void testGetAuthorizationActivityIntentWithSilentFlow() {
        // Create parameters with silent flow enabled
        final AuthorizationActivityParameters silentFlowParameters = new AuthorizationActivityParameters(
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
                null,
                10000L  // silent flow enabled
        );

        final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(
                silentFlowParameters
        );

        // Verify it creates SilentAuthorizationActivity
        assertNotNull(resultIntent.getComponent());
        assertEquals(SilentAuthorizationActivity.class.getName(), resultIntent.getComponent().getClassName());
        assertEquals(authIntent, resultIntent.getParcelableExtra(AUTH_INTENT));
        assertEquals(redirectUri, resultIntent.getStringExtra(REDIRECT_URI));
        assertEquals(authorizationAgent, resultIntent.getSerializableExtra(AUTHORIZATION_AGENT));
        assertEquals(webViewZoomEnabled, resultIntent.getBooleanExtra(WEB_VIEW_ZOOM_ENABLED, false));
        assertEquals(webViewZoomControlsEnabled, resultIntent.getBooleanExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, false));
        assertEquals(sourceLibraryName, resultIntent.getStringExtra(PRODUCT));
        assertEquals(sourceLibraryVersion, resultIntent.getStringExtra(VERSION));
        assertEquals(requestUrl, resultIntent.getStringExtra(REQUEST_URL));
        assertEquals(10000L, resultIntent.getLongExtra(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, 0));
    }

    @Test
    public void testGetAuthorizationFragmentFromStartIntentWebView() {
        // Create intent with silent flow enabled
        final Intent intent = new Intent();
        intent.putExtra(AUTHORIZATION_AGENT, AuthorizationAgent.WEBVIEW);

        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(intent);

        assertEquals(WebViewAuthorizationFragment.class, fragment.getClass());
    }

    @Test
    public void testGetAuthorizationFragmentFromStartIntentWebViewWithSilentFlow() {
        // Create intent with silent flow enabled
        final Intent silentFlowIntent = new Intent();
        silentFlowIntent.putExtra(AUTHORIZATION_AGENT, AuthorizationAgent.WEBVIEW);
        silentFlowIntent.putExtra(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, 10000L); // 10 seconds timeout

        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(silentFlowIntent);

        // Verify it creates SilentWebViewAuthorizationFragment for WebView with silent flow
        assertEquals(SilentWebViewAuthorizationFragment.class, fragment.getClass());
    }

    @Test
    public void testGetAuthorizationFragmentFromStartIntentWithSilentFlowNonWebView() {
        // Create intent with silent flow enabled but non-WebView agent
        final Intent silentFlowIntent = new Intent();
        silentFlowIntent.putExtra(AUTHORIZATION_AGENT, AuthorizationAgent.BROWSER);
        silentFlowIntent.putExtra(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, 10000L);

        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(silentFlowIntent);

        // Verify it creates BrowserAuthorizationFragment even with silent flow when not WebView
        assertEquals(BrowserAuthorizationFragment.class, fragment.getClass());
    }

    @Test
    public void testSwitchBrowserEnabled_noBrowser_urlUnchanged() {
        // No browsers installed in Robolectric → selectBrowser returns null → URL unchanged
        final AuthorizationActivityParameters params = new AuthorizationActivityParameters(
                context, authIntent, requestUrl, redirectUri, requestHeaders,
                authorizationAgent, webViewZoomEnabled, webViewZoomControlsEnabled,
                sourceLibraryName, sourceLibraryVersion, null, null, false, true
        );

        final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(params);
        assertEquals(requestUrl, resultIntent.getStringExtra(REQUEST_URL));
        assertFalse(resultIntent.getStringExtra(REQUEST_URL).contains("switch_browser"));
    }

    @Test
    public void testSwitchBrowserEnabled_browserAvailable_appendsParam() {
        // Register a handler for the switch_browser_resume redirect in the shadow PackageManager
        final ShadowPackageManager shadowPm = Shadows.shadowOf(context.getPackageManager());
        final Intent resumeIntent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("msauth://example.com/redirect/switch_browser_resume"));
        resumeIntent.addCategory(Intent.CATEGORY_DEFAULT);
        resumeIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        resumeIntent.setPackage(context.getPackageName());
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = context.getPackageName();
        resolveInfo.activityInfo.name = "com.microsoft.identity.common.internal.providers.oauth2.SwitchBrowserRedirectActivity";
        shadowPm.addResolveInfoForIntent(resumeIntent, resolveInfo);

        final Browser mockBrowser = new Browser("com.android.chrome", Collections.singleton("hash"), "100", true);
        try (MockedConstruction<AndroidBrowserSelector> ignored = mockConstruction(
                AndroidBrowserSelector.class,
                (mock, ctx) -> when(mock.selectBrowser(any(), any())).thenReturn(mockBrowser)
        )) {
            final AuthorizationActivityParameters params = new AuthorizationActivityParameters(
                    context, authIntent, requestUrl, redirectUri, requestHeaders,
                    authorizationAgent, webViewZoomEnabled, webViewZoomControlsEnabled,
                    sourceLibraryName, sourceLibraryVersion, null, null, false, true
            );

            final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(params);
            assertTrue(resultIntent.getStringExtra(REQUEST_URL).contains("switch_browser=1"));
        }
    }

    @Test
    public void testSwitchBrowserEnabled_browserAvailable_noManifestEntry_urlUnchanged() {
        // Browser available but NO manifest handler registered → URL unchanged
        final Browser mockBrowser = new Browser("com.android.chrome", Collections.singleton("hash"), "100", true);
        try (MockedConstruction<AndroidBrowserSelector> ignored = mockConstruction(
                AndroidBrowserSelector.class,
                (mock, ctx) -> when(mock.selectBrowser(any(), any())).thenReturn(mockBrowser)
        )) {
            final AuthorizationActivityParameters params = new AuthorizationActivityParameters(
                    context, authIntent, requestUrl, redirectUri, requestHeaders,
                    authorizationAgent, webViewZoomEnabled, webViewZoomControlsEnabled,
                    sourceLibraryName, sourceLibraryVersion, null, null, false, true
            );

            final Intent resultIntent = AuthorizationActivityFactory.getAuthorizationActivityIntent(params);
            assertFalse(resultIntent.getStringExtra(REQUEST_URL).contains("switch_browser"));
        }
    }
}
