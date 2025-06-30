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

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ReAttachPrtHeaderHandler;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.shadows.ShadowProcessUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AUTHENTICATOR_MFA_LINKING_PREFIX;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PLAY_STORE_INSTALL_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import io.opentelemetry.api.trace.Span;


@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryWebViewClientTest {
    private WebView mMockWebView;
    private AzureActiveDirectoryWebViewClient mWebViewClient;
    private Context mContext;
    private Activity mActivity;
    private static final String TEST_REDIRECT_URI = "abc12";

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
    private static final String TEST_PLAYSTORE_FOR_BROKER_APP_URL = "https://play.google.com/store/apps/details?id=com.azure.authenticator";
    private static final String TEST_INVALID_URL = "https://some.invalid.url";
    private static final String TEST_MSA_HEADER_FORWARDING_POSITIVE_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TEST_MSA_HEADER_FORWARDING_NEGATIVE_URL = "https://login.blah.com/oauth20_authorize.srf";

    private static final String TEST_NONCE_REDIRECT_URL = "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize?&sso_nonce=ABCD";
    private static final String TEST_CROSS_CLOUD_REDIRECT_URL = "https://login.microsoftonline.us/organizations/oAuth2/v2.0/authorize?x=10";
    private static final String TEST_PUBLIC_CLOUD_REDIRECT_URL = "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize?x=10";
    private static final String TEST_PASSKEY_REDIRECT_URL = "http-auth:PassKey?challenge=challenge&version=1.0&submitUrl=https://login.microsoftonline.com/common/credential?passKeyAuth=1.0%2fpasskey&context=&relyingPartyIdentifier=login.microsoft.com&allowedCredentials=somevalue";
    private static final String TEST_INTENT_INSTALL_BROKER_REDIRECT_URL = "intent://play.google.com/store/apps/details?id=com.azure.authenticator&referrer=%20adjust_reftag%3Dc6f1p4ErudH2C%26utm_source%3DLanding%2BPage%2BOrganic%2B-%2Bapp%2Bstore%2Bbadges%26utm_campaign%3Dappstore_android&pcampaignid=web_auto_redirect&web_logged_in=0&redirect_entry_point=dp#Intent;scheme=https;action=android.intent.action.VIEW;package=com.android.vending;end";

    private static final String TEST_WEB_CP_ENROLLMENT_URL = "https://enterprise.google.com/android/enroll";

    @Before
    public void setup() throws ClientException {
        mContext = ApplicationProvider.getApplicationContext();
        mMockWebView = new WebView(mContext);
        mActivity = Robolectric.buildActivity(Activity.class).get();
        mWebViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                new IAuthorizationCompletionCallback() {
                    @Override
                    public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {

                    }

                    @Override
                    public void setPKeyAuthStatus(boolean status) {
                        return;
                    }
                },
                new OnPageLoadedCallback() {
                    @Override
                    public void onPageLoaded(final String url) {
                        return;
                    }
                },
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserRequestHandler.class),
                "homeTenantId");
        HashMap<String, String> dummyHeaders = new HashMap<>();
        dummyHeaders.put("key", "value");
        mWebViewClient.setRequestHeaders(dummyHeaders);
        mWebViewClient.setRequestUrl(TEST_PUBLIC_CLOUD_REDIRECT_URL);
        if (!AzureActiveDirectory.isInitialized()) {
            AzureActiveDirectory.performCloudDiscovery();
        }
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
    public void testUrlOverrideHandlesPlayStoreRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAYSTORE_FOR_BROKER_APP_URL));
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
    public void testUrlOverrideHandlesNonceRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_NONCE_REDIRECT_URL));
    }

    @Test
    public void testUrlOverrideHandlesCrossCloudRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_CROSS_CLOUD_REDIRECT_URL));
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testUrlOverrideHandleWebCPEnrollmentUrlEnabled() {
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        assertTrue(mockWebViewClient.isWebCpInWebviewFeatureEnabled(TEST_WEB_CP_ENROLLMENT_URL));
        assertTrue(mockWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEB_CP_ENROLLMENT_URL));
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testUrlOverrideHandleWebCPEnrollmentUrlDisabled() {
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(false);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        assertFalse(mockWebViewClient.isWebCpInWebviewFeatureEnabled(TEST_WEB_CP_ENROLLMENT_URL));
        assertFalse(mockWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEB_CP_ENROLLMENT_URL));
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testLoadDeviceCaUrlInWebView() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockWebview).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testLoadDeviceCaUrlInBrowser() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(false);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockWebview, Mockito.never()).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testLoadDeviceCaUrlInBrowserInBrokelessFlow() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockFlightsProvider, Mockito.never()).isFlightEnabled(Mockito.any());
        Mockito.verify(mockWebview, Mockito.never()).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testProcessCloudRedirectAndPrtHeaderInternalSuccess() {
        ReAttachPrtHeaderHandler mockCrossCloudChallengeHandler = Mockito.mock(ReAttachPrtHeaderHandler.class);
        try {
            mWebViewClient.reAttachPrtHeader(TEST_CROSS_CLOUD_REDIRECT_URL, mockCrossCloudChallengeHandler, mMockWebView, "methodTag", Span.current());
        } catch (Exception e) {
            Assert.fail("Unexpected exception occured " + e);
        }
    }

    @Test
    public void testProcessCloudRedirectAndPrtHeaderInternalException() {
        ReAttachPrtHeaderHandler mockReAttachPrtHandler = Mockito.mock(ReAttachPrtHeaderHandler.class);
        WebView mockWebView = Mockito.mock(WebView.class);
        Mockito.doThrow(new RuntimeException("Test Exception")).when(mockReAttachPrtHandler).processChallenge(TEST_CROSS_CLOUD_REDIRECT_URL);
        try {
            mWebViewClient.reAttachPrtHeader(TEST_CROSS_CLOUD_REDIRECT_URL, mockReAttachPrtHandler, mockWebView, "methodTag", Span.current());
            Mockito.verify(mockReAttachPrtHandler, Mockito.times(1)).processChallenge(TEST_CROSS_CLOUD_REDIRECT_URL);
            Mockito.verify(mockWebView).loadUrl(Mockito.anyString());
        } catch (Exception e) {
            Assert.fail("Failure is not expected. We should have caught the exception and ignored it. " + e);
        }
    }

    public void testUrlOverrideHandlesIntentRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INTENT_INSTALL_BROKER_REDIRECT_URL));
    }

    public void setTestPasskeyRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PASSKEY_REDIRECT_URL));
    }

    @Test
    public void testPasskeyActivityCastingNoException() {
        try {
            // Putting an explicit non-AuthorizationActivity activity here to test that an exception won't be thrown.
            mActivity = Robolectric.buildActivity(DualScreenActivity.class).get();
            mWebViewClient = new AzureActiveDirectoryWebViewClient(
                    mActivity,
                    new IAuthorizationCompletionCallback() {
                        @Override
                        public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {

                        }

                        @Override
                        public void setPKeyAuthStatus(boolean status) {
                            return;
                        }
                    },
                    new OnPageLoadedCallback() {
                        @Override
                        public void onPageLoaded(final String url) {
                            return;
                        }
                    },
                    TEST_REDIRECT_URI,
                    Mockito.mock(SwitchBrowserRequestHandler.class),
                    "homeTenantId");
            mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PASSKEY_REDIRECT_URL);
        } catch (ClassCastException e) {
            Assert.fail("Failure is not expected. The class checks should have prevented this." + e);
        } catch (Exception e) {
            Assert.fail("Failure is not expected." + e);
        }
    }
}
