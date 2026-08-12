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

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.MAM_CA_INSTALL_REFERRER_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.webkit.WebView;

import androidx.fragment.app.FragmentActivity;

import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator;
import com.microsoft.identity.common.java.providers.MamInstallReferrerBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.time.Duration;
import java.util.HashMap;

/**
 * Verifies that the host's MAM-CA install-referrer opt-in actually reaches the WebView client that
 * {@link WebViewAuthorizationFragment} builds.
 * <p>
 * This covers the embedded-WebView launch path, one of the two places the referrer is applied. The
 * other - the base-class {@code decorateAppLinkForMamCaInstall} helper - is covered by
 * {@link AuthorizationFragmentInstallReferrerTest}, and the decoration rules themselves by
 * {@code AzureActiveDirectoryWebViewClientTest}. What is unique here is the <em>wiring</em>: those
 * suites construct their subject directly, so none of them would notice if the fragment stopped
 * handing its restored opt-in to the client.
 * <p>
 * Each test drives the real chain - instance-state bundle, {@code extractState}, the client
 * constructor, a launched install intent - rather than setting the field or the client up by hand.
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewAuthorizationFragmentInstallReferrerTest {

    /** A Play link for Company Portal carrying no referrer of its own. */
    private static final String ENCODED_CP_APP_LINK =
            "https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.microsoft.windowsintune.companyportal";

    /** A broker-install redirect the server has marked as a MAM Conditional Access install. */
    private static final String MAM_CA_INSTALL_REDIRECT =
            "msauth://wpj/?username=someone%40contoso.onmicrosoft.com&intuneAppProtection=1&app_link="
                    + ENCODED_CP_APP_LINK;

    private static final String TEST_REQUEST_URL = "https://login.microsoftonline.com/common/oauth2/authorize";
    private static final String TEST_REDIRECT_URI = "msauth://com.microsoft.identity.client.sample.local";

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(FragmentActivity.class).get();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * @param optIn the value the host would have put in the launch bundle, or null to leave the key
     *              out entirely - which is what MSAL and the broker do.
     * @return the instance-state bundle the fragment is started with.
     */
    private Bundle instanceState(final Boolean optIn) {
        final Bundle state = new Bundle();
        state.putString(REQUEST_URL, TEST_REQUEST_URL);
        state.putString(REDIRECT_URI, TEST_REDIRECT_URI);
        state.putSerializable(REQUEST_HEADERS, new HashMap<String, String>());
        if (optIn != null) {
            state.putBoolean(MAM_CA_INSTALL_REFERRER_ENABLED, optIn);
        }
        return state;
    }

    /**
     * Restores a fragment from an instance-state bundle exactly as {@code onCreate} does, then has
     * it build its WebView client exactly as {@code onCreateView} does.
     *
     * @return the client the fragment built.
     */
    private AzureActiveDirectoryWebViewClient clientBuiltFromState(final Boolean optIn) {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        // Supplied up front so the client construction does not need an attached activity to lazily
        // create one; the coordinator plays no part in install-referrer decoration.
        fragment.setSwitchBrowserProtocolCoordinator(mock(SwitchBrowserProtocolCoordinator.class));
        fragment.extractState(instanceState(optIn));

        final AzureActiveDirectoryWebViewClient client =
                fragment.createAADWebViewClient((FragmentActivity) mActivity);
        assertNotNull("The fragment must have built a WebView client", client);
        return client;
    }

    /**
     * Feeds a broker-install redirect to the client and returns the install intent it launched.
     * <p>
     * The launch is posted a second out so the calling activity can register its broker-result
     * receiver first, so the main looper has to be advanced past that delay before the intent
     * exists. Robolectric pauses the looper by default, which is what makes this observable.
     */
    private Intent launchInstallAndCaptureIntent(final AzureActiveDirectoryWebViewClient client) {
        client.setRequestUrl(TEST_REQUEST_URL);
        assertTrue("A broker-install redirect must be handled by the WebView client",
                client.shouldOverrideUrlLoading(mock(WebView.class), MAM_CA_INSTALL_REDIRECT));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        return Shadows.shadowOf(mActivity).getNextStartedActivity();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * The wiring under test: a host that opted in gets its package named as the Play install
     * referrer on the embedded-WebView path. Fails if the fragment stops handing its restored
     * opt-in to the client.
     */
    @Test
    public void testCreateWebViewClient_hostOptedIn_clientTagsInstallWithReferrer() {
        final Intent launched = launchInstallAndCaptureIntent(clientBuiltFromState(true));

        assertNotNull("Expected the Company Portal install link to be launched", launched);
        assertTrue("The opt-in must reach the WebView client and tag the install, but was: "
                        + launched.getDataString(),
                launched.getDataString().contains(
                        MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + mActivity.getPackageName()));
    }

    /**
     * The mirror case, and the one MSAL and the broker actually get: no opt-in in the bundle means
     * the install link is launched exactly as the server sent it.
     */
    @Test
    public void testCreateWebViewClient_optInAbsentFromState_clientLeavesInstallUnchanged() {
        final Intent launched = launchInstallAndCaptureIntent(clientBuiltFromState(null));

        assertNotNull("The install must still be launched without the opt-in", launched);
        assertFalse("Without the opt-in nothing may be appended to the install link, but was: "
                        + launched.getDataString(),
                launched.getDataString().contains(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "="));
    }

    /**
     * An explicit {@code false} differs from an absent key only in the bundle; the launched link
     * must be identical. Guards against the opt-in being read with an inverted default.
     */
    @Test
    public void testCreateWebViewClient_hostOptedOut_clientLeavesInstallUnchanged() {
        final Intent launched = launchInstallAndCaptureIntent(clientBuiltFromState(false));

        assertNotNull("The install must still be launched when the host opted out", launched);
        assertFalse("An explicit opt-out must not tag the install, but was: "
                        + launched.getDataString(),
                launched.getDataString().contains(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "="));
    }
}
