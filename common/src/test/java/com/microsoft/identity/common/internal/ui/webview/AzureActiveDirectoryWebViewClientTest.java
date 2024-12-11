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
package com.microsoft.identity.common.internal.ui.webview;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AUTHENTICATOR_MFA_LINKING_PREFIX;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PLAY_STORE_INSTALL_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;


@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryWebViewClientTest {
    private WebView mMockWebView;
    private AzureActiveDirectoryWebViewClient mWebViewClient;
    private static final String TEST_REDIRECT_URI = "msauth://test.redirect.url";

    // Test strings initialized.
    private static final String TEST_PLAY_STORE_INSTALL_AUTH_APP_URL =
            PLAY_STORE_INSTALL_PREFIX + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
    private static final String TEST_PLAY_STORE_INSTALL_CP_URL =
            PLAY_STORE_INSTALL_PREFIX + COMPANY_PORTAL_APP_PACKAGE_NAME;
    private static final String TEST_PLAY_STORE_INSTALL_INVALID_APP =
            PLAY_STORE_INSTALL_PREFIX + "com.azure.xyz";
    private static final String AUTHENTICATOR_MFA_LINKING_INVALID_URI =
            AUTHENTICATOR_MFA_LINKING_PREFIX + "xyz";
    private static final String TEST_SSL_PROTECTION_HTTP_URL = "http://foo";
    private static final String TEST_SSL_PROTECTION_FTP_URL = "ftp://foo";
    private static final String TEST_REDIRECT_URL = "ABC12/xyz";
    private static final String TEST_WEBSITE_REQUEST_URL = "browser://abcxyz/a";
    private static final String TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER = "browser://abcxyz/xyz&ismdmurl=1";
    private static final String TEST_INSTALL_REQUEST_URL = "msauth://wpj/?username=someusername%somedomain.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator%26referrer%3dcom.msft.identity.client.sample.local";
    private static final String TEST_DEVICE_REGISTRATION_URL = "msauth://wpj/?username=someusername%somedomain.onmicrosoft.com";
    private static final String TEST_BLANK_PAGE_REQUEST_URL = "about:blank";
    private static final String TEST_PKEY_AUTH_URL = "urn:http-auth:PKeyAuth/xyz";
    private static final String TEST_WEB_CP_URL = "companyportal://abc/123";
    private static final String TEST_INVALID_URL = "https://play.google.com/store/apps/details?id=com.azure.authenticator";
    private static final String TEST_MSA_HEADER_FORWARDING_POSITIVE_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TEST_MSA_HEADER_FORWARDING_NEGATIVE_URL = "https://login.blah.com/oauth20_authorize.srf";
    private static final String SWITCH_BROWSER_CODE = "switchbrowsercode";
    private static final String SWITCH_BROWSER_ACTION_URI = "login.microsoftonline.com";
    private static final String SWITCH_BROWSER_ACTION = "switch_browser";
    private static final String SWITCH_BROWSER_ACTION_URI_PATHS = "/switchbrowser/process";
    private static final String TEST_SWITCH_BROWSER_REDIRECT_URL =
            TEST_REDIRECT_URI + "?" +
                    AuthenticationConstants.SWITCH_BROWSER.ACTION_URI + "=" + SWITCH_BROWSER_ACTION_URI + SWITCH_BROWSER_ACTION_URI_PATHS + "&" +
                    AuthenticationConstants.SWITCH_BROWSER.CODE + "=" + SWITCH_BROWSER_CODE + "&" +
                    AuthenticationConstants.SWITCH_BROWSER.ACTION + "=" + SWITCH_BROWSER_ACTION;

    private static final String TEST_SWITCH_BROWSER_URL =
            "https://" + SWITCH_BROWSER_ACTION_URI + SWITCH_BROWSER_ACTION_URI_PATHS + "?" +
                    AuthenticationConstants.SWITCH_BROWSER.CODE + "=" + SWITCH_BROWSER_CODE;

    @Before
    public void setup() {
        final AuthorizationActivity activity = mock(AuthorizationActivity.class);
        final WebViewAuthorizationFragment fragment = mock(WebViewAuthorizationFragment.class);
        final Context context = ApplicationProvider.getApplicationContext();
        when(activity.getApplicationContext()).thenReturn(context);
        when(activity.getFragment()).thenReturn(fragment);
        when(activity.getPackageManager()).thenReturn(context.getPackageManager());
        when(fragment.launchWebBrowserIntent(Uri.parse(TEST_SWITCH_BROWSER_URL))).thenReturn(true);
        mMockWebView = new WebView(context);
        mWebViewClient = new AzureActiveDirectoryWebViewClient(
                activity,
                new IAuthorizationCompletionCallback() {
                    @Override
                    public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {

                    }

                    @Override
                    public void setPKeyAuthStatus(boolean status) {
                    }
                },
                url -> {
                },
                TEST_REDIRECT_URI);
        HashMap<String, String> dummyHeaders = new HashMap<>();
        dummyHeaders.put("key", "value");
        mWebViewClient.setRequestHeaders(dummyHeaders);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlOverrideHandlesEmptyString() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlOverrideHandlesNullString() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, (String) null));
    }

    @Test
    public void testUrlOverrideHandlesPkeyAuthUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PKEY_AUTH_URL));
    }

    @Test
    public void testUrlOverrideHandlesWebsiteRequestUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEBSITE_REQUEST_URL));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER));
    }

    @Test
    public void testUrlOverrideHandlesInstallRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INSTALL_REQUEST_URL));
    }

    @Test
    public void testUrlOverrideHandlesDeviceRegistrationRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_DEVICE_REGISTRATION_URL));
    }

    @Test
    public void testUrlOverrideHandlesWebCpUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEB_CP_URL));
    }

    @Test
    public void testUrlOverrideHandlesRedirectUriString() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_REDIRECT_URL));
    }

    @Test
    public void testUrlOverrideHandlesPlayStoreRedirect() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAY_STORE_INSTALL_AUTH_APP_URL));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAY_STORE_INSTALL_CP_URL));
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAY_STORE_INSTALL_INVALID_APP));
    }

    @Test
    public void testUrlOverrideHandlesAuthAppMFAUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, AUTHENTICATOR_MFA_LINKING_INVALID_URI));
    }

    @Test
    public void testUrlOverrideHandlesSSLProtectionCheck() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_SSL_PROTECTION_HTTP_URL));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_SSL_PROTECTION_FTP_URL));
    }

    @Test
    public void testUrlOverrideHandlesBlankPageRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_BLANK_PAGE_REQUEST_URL));
    }

    @Test
    public void testUrlOverrideHandlesInvalidUrl() {
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INVALID_URL));
    }

    @Test
    public void testUrlOverrideHandlesHeaderForwardingRequiredUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_MSA_HEADER_FORWARDING_POSITIVE_URL));
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_MSA_HEADER_FORWARDING_NEGATIVE_URL));
    }

    @Test
    public void testUrlOverrideHandlesSwitchBrowserValidURL() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_SWITCH_BROWSER_REDIRECT_URL));
    }

    @Test
    public void testUrlOverrideHandlesSwitchBrowserMissingCode() {
        final String switchBrowserInvalidUrl = TEST_REDIRECT_URI + "?" +
                AuthenticationConstants.SWITCH_BROWSER.ACTION_URI + "=" + SWITCH_BROWSER_ACTION_URI + SWITCH_BROWSER_ACTION_URI_PATHS + "&" +
                AuthenticationConstants.SWITCH_BROWSER.CODE + "=" + "&" +
                AuthenticationConstants.SWITCH_BROWSER.ACTION + "=" + SWITCH_BROWSER_ACTION;
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, switchBrowserInvalidUrl));
    }

    @Test
    public void testUrlOverrideHandlesSwitchBrowserMissingActionUrl() {
        final String switchBrowserInvalidUrl = TEST_REDIRECT_URI + "?" +
                AuthenticationConstants.SWITCH_BROWSER.ACTION_URI + "=" + "&" +
                AuthenticationConstants.SWITCH_BROWSER.CODE + "=" + SWITCH_BROWSER_CODE + "&" +
                AuthenticationConstants.SWITCH_BROWSER.ACTION + "=" + SWITCH_BROWSER_ACTION;
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, switchBrowserInvalidUrl));
    }
}
