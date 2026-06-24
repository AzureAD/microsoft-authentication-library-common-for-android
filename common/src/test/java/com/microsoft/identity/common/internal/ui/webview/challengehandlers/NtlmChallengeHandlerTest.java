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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.Activity;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for the request-origin display logic in {@link NtlmChallengeHandler}. The host/realm formatting
 * and the flight gate are exercised against a stubbed {@link Activity} so the logic is validated without
 * depending on resource resolution or dialog inflation (covered by instrumented tests).
 */
@RunWith(RobolectricTestRunner.class)
public class NtlmChallengeHandlerTest {
    private static final String TEST_HOST = "login.example.com";
    private static final String TEST_REALM = "ExampleRealm";

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testGetOriginText_containsHostAndRealm() {
        final NtlmChallengeHandler handler = new NtlmChallengeHandler(stubActivity(), mockCallback());
        final NtlmChallenge challenge = new NtlmChallenge(null, null, TEST_HOST, TEST_REALM);

        final String originText = handler.getOriginText(challenge);

        assertTrue(originText.contains(TEST_HOST));
        assertTrue(originText.contains(TEST_REALM));
    }

    @Test
    public void testIsHttpAuthOriginDisplayEnabled_trueWhenFlightEnabled() {
        setHttpAuthOriginDisplayFlight(true);
        final NtlmChallengeHandler handler = new NtlmChallengeHandler(stubActivity(), mockCallback());

        assertTrue(handler.isHttpAuthOriginDisplayEnabled());
    }

    @Test
    public void testIsHttpAuthOriginDisplayEnabled_falseWhenFlightDisabled() {
        setHttpAuthOriginDisplayFlight(false);
        final NtlmChallengeHandler handler = new NtlmChallengeHandler(stubActivity(), mockCallback());

        assertFalse(handler.isHttpAuthOriginDisplayEnabled());
    }

    /**
     * Builds a mock {@link Activity} whose origin-string lookups echo their argument, so origin-text
     * formatting can be verified without resolving Android string resources.
     */
    private Activity stubActivity() {
        final Activity activity = Mockito.mock(Activity.class);
        when(activity.getString(eq(R.string.http_auth_dialog_origin_host), any()))
                .thenAnswer(invocation -> "Host: " + invocation.getArgument(1));
        when(activity.getString(eq(R.string.http_auth_dialog_origin_realm), any()))
                .thenAnswer(invocation -> "Realm: " + invocation.getArgument(1));
        return activity;
    }

    private IAuthorizationCompletionCallback mockCallback() {
        return Mockito.mock(IAuthorizationCompletionCallback.class);
    }

    private void setHttpAuthOriginDisplayFlight(final boolean enabled) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_HTTP_AUTH_ORIGIN_DISPLAY)).thenReturn(enabled);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
    }
}
