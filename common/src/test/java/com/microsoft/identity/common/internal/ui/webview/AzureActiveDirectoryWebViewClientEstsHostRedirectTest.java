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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ReAttachPrtHeaderHandler;
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

import io.opentelemetry.api.trace.Span;

/**
 * Tests for eSTS cloud host detection and PRT header re-attachment in
 * {@link AzureActiveDirectoryWebViewClient}.
 */
@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryWebViewClientEstsHostRedirectTest {

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
                    }
                },
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserRequestHandler.class),
                "homeTenantId",
                false);
        final HashMap<String, String> dummyHeaders = new HashMap<>();
        dummyHeaders.put("key", "value");
        // Include PRT header so tests that verify re-attachment pass the hasPrtHeaderAttached() guard.
        dummyHeaders.put(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER, "dummy-prt-value");
        mWebViewClient.setRequestHeaders(dummyHeaders);
        mWebViewClient.setRequestUrl(TEST_ESTS_URL);
        AzureActiveDirectory.ensureCloudDiscovery();
    }

    @After
    public void cleanUp() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    // ------- Flight DISABLED regression tests -------
    // When the ENABLE_PRT_HEADER_FOR_ESTS_HOST_REDIRECT flight is OFF, all eSTS host URLs
    // must fall through to the default path (return false) — exactly the same behavior as
    // before this feature existed.  reAttachPrtHeader must never be called.

    /**
     * Helper: initializes the flights manager with the eSTS host PRT flight set to the given value.
     */
    private void initFlightsWithEstsPrtFlight(boolean enabled) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_PRT_HEADER_FOR_ESTS_HOST_REDIRECT))
                .thenReturn(enabled);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
    }

    /**
     * When the flight is disabled, every URL variant must fall through (return false)
     * and reAttachPrtHeader must never be called — same behavior as before this feature existed.
     */
    @Test
    public void flightDisabled_allUrlVariants_fallThrough_returnsFalse() {
        initFlightsWithEstsPrtFlight(false);

        final String[][] urlsWithLabels = {
                {"eSTS host /authorize",     TEST_ESTS_URL},
                {"eSTS /authorize with client_id", "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=test-client-id&response_type=code"},
                {"China cloud",              "https://login.chinacloudapi.cn/organizations/oauth2/v2.0/authorize?x=10"},
                {"US Gov cloud",             "https://login.microsoftonline.us/organizations/oauth2/v2.0/authorize?x=10"},
                {"eSTS /token path",         "https://login.microsoftonline.com/common/oauth2/v2.0/token?code=abc"},
                {"eSTS KMSI path",           "https://login.microsoftonline.com/common/oauth2/v2.0/authorize/kmsi"},
                {"non-eSTS HTTPS",           TEST_NON_ESTS_URL},
        };

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        for (final String[] entry : urlsWithLabels) {
            final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, entry[1]);
            assertFalse(entry[0] + " must fall through when flight is disabled", result);
        }

        // Across all URLs, reAttachPrtHeader must never have been invoked.
        verify(spyClient, never()).reAttachPrtHeader(
                Mockito.anyString(),
                Mockito.any(ReAttachPrtHeaderHandler.class),
                Mockito.any(WebView.class),
                Mockito.anyString(),
                Mockito.any(Span.class)
        );
    }

    // ------- Flight ENABLED tests -------
    // When the flight is ON, shouldReAttachPrtForEstsHost returns true iff:
    //   isEstsCloudHost(url) AND hasPrtHeaderAttached()
    //   AND (!url.contains("/authorize") || hasKnownClientId(url))
    //
    // hasKnownClientId(url):
    //   - redirectClientId == null → false
    //   - redirectClientId matches originalClientId (from mRequestUrl) → true
    //   - redirectClientId matches BROKER_CLIENT_ID → true
    //   - exception during extraction → true

    private static final String TEST_ORIGINAL_CLIENT_ID = "test-original-client-id";
    private static final String BROKER_CLIENT_ID = "29d9ed98-a469-4536-ade2-f981bc1d605e";

    // -- Non-/authorize paths: should always be handled (no client_id check) --

    @Test
    public void flightEnabled_nonAuthorizePaths_handled() {
        initFlightsWithEstsPrtFlight(true);

        final String[][] handledUrls = {
                {"eSTS /token endpoint", "https://login.microsoftonline.com/common/oauth2/v2.0/token?code=abc"},
                {"Sovereign cloud /token", "https://login.chinacloudapi.cn/common/oauth2/v2.0/token?code=abc"},
        };

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        for (final String[] entry : handledUrls) {
            final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, entry[1]);
            assertTrue(entry[0] + " should be handled", result);
        }

        // reAttachPrtHeader must have been called for each handled URL.
        verify(spyClient, Mockito.times(handledUrls.length)).reAttachPrtHeader(
                Mockito.anyString(),
                Mockito.any(ReAttachPrtHeaderHandler.class),
                Mockito.any(WebView.class),
                Mockito.anyString(),
                Mockito.any(Span.class)
        );
    }

    // -- /authorize path with known client_id (original or broker) → handled --

    @Test
    public void flightEnabled_authorizePath_knownClientId_handled() {
        initFlightsWithEstsPrtFlight(true);

        // Set request URL with a client_id so getClientIdFromRequestUrl() returns it.
        mWebViewClient.setRequestUrl(
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=" + TEST_ORIGINAL_CLIENT_ID);

        final String[][] handledUrls = {
                {"matching original client_id",
                        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=" + TEST_ORIGINAL_CLIENT_ID + "&response_type=code"},
                {"broker client_id",
                        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=" + BROKER_CLIENT_ID},
        };

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        for (final String[] entry : handledUrls) {
            final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, entry[1]);
            assertTrue("/authorize with " + entry[0] + " should be handled", result);
        }

        verify(spyClient, Mockito.times(handledUrls.length)).reAttachPrtHeader(
                Mockito.anyString(),
                Mockito.any(ReAttachPrtHeaderHandler.class),
                Mockito.any(WebView.class),
                Mockito.anyString(),
                Mockito.any(Span.class)
        );
    }

    // -- /authorize path with unrecognized/missing client_id → falls through --

    @Test
    public void flightEnabled_authorizePath_unrecognizedClientId_fallsThrough() {
        initFlightsWithEstsPrtFlight(true);

        mWebViewClient.setRequestUrl(
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=" + TEST_ORIGINAL_CLIENT_ID);

        // All these /authorize URLs should fail the hasKnownClientId check:
        //  - unknown client_id doesn't match original or broker
        //  - missing client_id returns null → false
        //  - KMSI path contains "/authorize" substring, and has no client_id
        final String[][] fallThroughUrls = {
                {"unknown client_id",
                        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=unknown-app-id"},
                {"no client_id param",
                        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?response_type=code"},
                {"KMSI path (contains /authorize, no client_id)",
                        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize/kmsi"},
        };

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        for (final String[] entry : fallThroughUrls) {
            final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, entry[1]);
            assertFalse(entry[0] + " should fall through", result);
        }

        verify(spyClient, never()).reAttachPrtHeader(
                Mockito.anyString(),
                Mockito.any(ReAttachPrtHeaderHandler.class),
                Mockito.any(WebView.class),
                Mockito.anyString(),
                Mockito.any(Span.class)
        );
    }

    // -- Non-eSTS host → falls through --

    @Test
    public void flightEnabled_nonEstsHost_returnsFalse() {
        initFlightsWithEstsPrtFlight(true);

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, TEST_NON_ESTS_URL);
        assertFalse("Non-eSTS host should fall through even with flight enabled", result);
    }

    // -- No PRT header → falls through --

    @Test
    public void flightEnabled_noPrtHeader_returnsFalse() {
        initFlightsWithEstsPrtFlight(true);

        // Remove PRT header from request headers.
        final HashMap<String, String> headersWithoutPrt = new HashMap<>();
        headersWithoutPrt.put("key", "value");
        mWebViewClient.setRequestHeaders(headersWithoutPrt);

        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, TEST_ESTS_URL);
        assertFalse("Without PRT header, eSTS host redirect should fall through", result);
    }

    // ------- setRequestUrl() / mLoginHint extraction tests -------

    @Test
    public void setRequestUrl_extractsLoginHint_whenPresent() {
        // Verifies that setRequestUrl populates mLoginHint. We check this indirectly:
        // set a request URL with login_hint AND client_id, then verify a redirect with
        // matching client_id is handled (which triggers processEstsHostRedirect with mLoginHint).
        initFlightsWithEstsPrtFlight(true);

        mWebViewClient.setRequestUrl(
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id="
                        + TEST_ORIGINAL_CLIENT_ID + "&login_hint=user%40contoso.com");

        // Redirect with matching client_id so shouldReAttachPrtForEstsHost → true.
        final String redirectUrl =
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=" + TEST_ORIGINAL_CLIENT_ID;
        final AzureActiveDirectoryWebViewClient spyClient = spy(mWebViewClient);
        final boolean result = spyClient.shouldOverrideUrlLoading(mMockWebView, redirectUrl);
        assertTrue("Expected eSTS host redirect to be handled after login_hint extraction", result);
    }

    @Test
    public void setRequestUrl_doesNotThrow_forEdgeCases() {
        // Neither missing login_hint nor malformed URL should throw.
        mWebViewClient.setRequestUrl(TEST_ESTS_URL);
        mWebViewClient.setRequestUrl(TEST_MALFORMED_URL);
    }
}
