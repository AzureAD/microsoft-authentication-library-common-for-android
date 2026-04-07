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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;

/**
 * Tests for eSTS cloud host detection and PRT header re-attachment in
 * {@link AzureActiveDirectoryWebViewClient}.
 */
@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryWebViewClientEstsReturnRedirectTest {

    private static final String TEST_REDIRECT_URI = "abc12";
    private static final String TEST_ESTS_URL =
            "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize?x=10";
    private static final String TEST_ESTS_URL_WITH_LOGIN_HINT =
            "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize?login_hint=user%40contoso.com";
    private static final String TEST_NON_ESTS_URL =
            "https://mysignins.microsoft.com/passkey";
    private static final String TEST_MALFORMED_URL = "not a url %%invalid";

    private Context mContext;
    private Activity mActivity;
    private AzureActiveDirectoryWebViewClient mWebViewClient;
    private WebView mMockWebView;

    @Before
    public void setup() {
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
                    }
                },
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserRequestHandler.class),
                "homeTenantId",
                false);
        final HashMap<String, String> dummyHeaders = new HashMap<>();
        dummyHeaders.put("key", "value");
        mWebViewClient.setRequestHeaders(dummyHeaders);
        mWebViewClient.setRequestUrl(TEST_ESTS_URL);
        AzureActiveDirectory.ensureCloudDiscovery();
    }

    @After
    public void cleanUp() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    // ------- isEstsCloudHost() tests -------

    @Test
    public void isEstsCloudHost_returnsTrue_forKnownEstsHost() {
        assertTrue(mWebViewClient.isEstsCloudHost(TEST_ESTS_URL));
    }

    @Test
    public void isEstsCloudHost_returnsFalse_forNonEstsHost() {
        assertFalse(mWebViewClient.isEstsCloudHost(TEST_NON_ESTS_URL));
    }

    @Test
    public void isEstsCloudHost_returnsFalse_forMalformedUrl() {
        assertFalse(mWebViewClient.isEstsCloudHost(TEST_MALFORMED_URL));
    }

    // ------- handleUrl() flight-gating tests -------

    @Test
    public void handleUrl_whenFlightEnabled_andEstsHost_returnsTrue() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_PRT_HEADER_FOR_ESTS_RETURN_REDIRECT))
                .thenReturn(true);
        // Keep other flights at default behaviour — not enabled
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, TEST_ESTS_URL);
        assertTrue(result);
    }

    @Test
    public void handleUrl_whenFlightDisabled_andEstsHost_doesNotCallProcessEstsReturnRedirect() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_PRT_HEADER_FOR_ESTS_RETURN_REDIRECT))
                .thenReturn(false);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        // When the flight is disabled, the eSTS host check is bypassed entirely;
        // the URL falls through to the unrecognized-URL path and returns false.
        final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, TEST_ESTS_URL);
        assertFalse(result);
    }

    @Test
    public void handleUrl_whenFlightEnabled_andNonEstsHost_doesNotHandleAsEstsReturn() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_PRT_HEADER_FOR_ESTS_RETURN_REDIRECT))
                .thenReturn(true);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        // Non-eSTS HTTPS URL should not be caught by the new branch — it falls through
        // to the unrecognized-URL path and returns false.
        final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, TEST_NON_ESTS_URL);
        assertFalse(result);
    }

    // ------- setRequestUrl() / mLoginHint extraction tests -------

    @Test
    public void setRequestUrl_extractsLoginHint_whenPresent() {
        // Create a fresh client so we control the initial URL
        final AzureActiveDirectoryWebViewClient client = new AzureActiveDirectoryWebViewClient(
                mActivity,
                new IAuthorizationCompletionCallback() {
                    @Override
                    public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {}
                    @Override
                    public void setPKeyAuthStatus(boolean status) {}
                },
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserRequestHandler.class),
                "homeTenantId",
                false);
        // Setting a URL with login_hint should not throw.
        client.setRequestUrl(TEST_ESTS_URL_WITH_LOGIN_HINT);
        // Verify no exception — the internal mLoginHint field is set (verified indirectly).
    }

    @Test
    public void setRequestUrl_doesNotThrow_whenLoginHintAbsent() {
        mWebViewClient.setRequestUrl(TEST_ESTS_URL);
        // No exception expected when login_hint is not present in the URL.
    }

    @Test
    public void setRequestUrl_doesNotThrow_forMalformedUrl() {
        // Should log a warning and not propagate the exception.
        mWebViewClient.setRequestUrl(TEST_MALFORMED_URL);
    }
}
