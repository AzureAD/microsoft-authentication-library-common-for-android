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
// The above copyright and this permission notice shall be included in
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationFragment.UrlStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

/**
 * Tests for URL-load tracking in {@link AuthorizationFragment} and its nested
 * {@link AuthorizationFragment.UrlStatus} class.
 */
@RunWith(RobolectricTestRunner.class)
public class AuthorizationFragmentUrlTrackingTest {

    /**
     * Minimal concrete subclass that allows direct access to the protected
     * tracking helpers without needing a full Fragment lifecycle.
     */
    private static class TestAuthorizationFragment extends AuthorizationFragment {

        // Expose protected methods for testing

        void trackUrl(final String url, final String loadingError, final String authError) {
            trackUrlStatus(url, loadingError, authError);
        }

        void updateLatest(final String loadingError, final String authError) {
            updateLatestUrlStatus(loadingError, authError);
        }

        Map<Integer, UrlStatus> getTracker() {
            return getUrlLoadTracker();
        }
    }

    private TestAuthorizationFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new TestAuthorizationFragment();
    }

    // -----------------------------------------------------------------------
    // UrlStatus.sanitizeUrl – tested indirectly via the package-private
    // constructor which calls sanitizeUrl on the supplied url.
    // -----------------------------------------------------------------------

    @Test
    public void testUrlStatus_aadGlobalHost_notRedacted() {
        final UrlStatus status = new UrlStatus(
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=abc",
                null, null);
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                status.getUrl());
    }

    @Test
    public void testUrlStatus_aadChinaHost_notRedacted() {
        final UrlStatus status = new UrlStatus(
                "https://login.partner.microsoftonline.cn/common/oauth2/v2.0/authorize?x=1",
                null, null);
        assertEquals("https://login.partner.microsoftonline.cn/common/oauth2/v2.0/authorize",
                status.getUrl());
    }

    @Test
    public void testUrlStatus_aadUsHost_notRedacted() {
        final UrlStatus status = new UrlStatus(
                "https://login.microsoftonline.us/common/oauth2/v2.0/authorize?x=1",
                null, null);
        assertEquals("https://login.microsoftonline.us/common/oauth2/v2.0/authorize",
                status.getUrl());
    }

    @Test
    public void testUrlStatus_msaHost_notRedacted() {
        final UrlStatus status = new UrlStatus(
                "https://login.live.com/oauth20_authorize.srf?client_id=abc",
                null, null);
        assertEquals("https://login.live.com/oauth20_authorize.srf", status.getUrl());
    }

    @Test
    public void testUrlStatus_microsoftComHost_notRedacted() {
        final UrlStatus status = new UrlStatus(
                "https://account.microsoft.com/some/path?query=value",
                null, null);
        assertEquals("https://account.microsoft.com/some/path", status.getUrl());
    }

    @Test
    public void testUrlStatus_nonAadHost_redacted() {
        final UrlStatus status = new UrlStatus(
                "https://evil.example.com/steal?token=secret",
                null, null);
        assertEquals("[REDACTED]", status.getUrl());
    }

    @Test
    public void testUrlStatus_queryParamsStripped_forAllowedHost() {
        final UrlStatus status = new UrlStatus(
                "https://login.microsoftonline.com/authorize?code=secret_code&state=abc",
                null, null);
        // Query params must be stripped; only scheme+host+path retained
        assertEquals("https://login.microsoftonline.com/authorize", status.getUrl());
    }

    @Test
    public void testUrlStatus_nullUrl_handledGracefully() {
        final UrlStatus status = new UrlStatus(null, null, null);
        assertNull(status.getUrl());
    }

    @Test
    public void testUrlStatus_emptyUrl_handledGracefully() {
        final UrlStatus status = new UrlStatus("", null, null);
        assertEquals("", status.getUrl());
    }

    @Test
    public void testUrlStatus_gettersReturnSuppliedValues() {
        final UrlStatus status = new UrlStatus(
                "https://login.microsoftonline.com/authorize",
                "loading error",
                "auth error");
        assertNotNull(status.getUrl());
        assertEquals("loading error", status.getLoadingError());
        assertEquals("auth error", status.getAuthError());
    }

    // -----------------------------------------------------------------------
    // AuthorizationFragment tracking methods
    // -----------------------------------------------------------------------

    @Test
    public void testTrackUrlStatus_addsEntryToTracker() {
        mFragment.trackUrl("https://login.microsoftonline.com/authorize", null, null);

        final Map<Integer, UrlStatus> tracker = mFragment.getTracker();
        assertEquals(1, tracker.size());
        assertNotNull(tracker.get(1));
        // URL is sanitized; query params stripped
        assertEquals("https://login.microsoftonline.com/authorize",
                tracker.get(1).getUrl());
    }

    @Test
    public void testTrackUrlStatus_multipleUrls_incrementsKeys() {
        mFragment.trackUrl("https://login.microsoftonline.com/step1", null, null);
        mFragment.trackUrl("https://login.microsoftonline.com/step2", null, null);

        final Map<Integer, UrlStatus> tracker = mFragment.getTracker();
        assertEquals(2, tracker.size());
        assertNotNull(tracker.get(1));
        assertNotNull(tracker.get(2));
    }

    @Test
    public void testUpdateLatestUrlStatus_updatesLatestEntry() {
        mFragment.trackUrl("https://login.microsoftonline.com/authorize", null, null);
        mFragment.updateLatest("network error", null);

        final UrlStatus entry = mFragment.getTracker().get(1);
        assertNotNull(entry);
        assertEquals("network error", entry.getLoadingError());
        assertNull(entry.getAuthError());
    }

    @Test
    public void testUpdateLatestUrlStatus_preservesUrl() {
        mFragment.trackUrl("https://login.microsoftonline.com/authorize", null, null);
        mFragment.updateLatest("error", null);

        final UrlStatus entry = mFragment.getTracker().get(1);
        assertNotNull(entry);
        assertEquals("https://login.microsoftonline.com/authorize", entry.getUrl());
    }

    @Test
    public void testUpdateLatestUrlStatus_noExistingEntry_noException() {
        // updateLatestUrlStatus with nothing tracked yet should not throw
        mFragment.updateLatest("some error", null);
        // Tracker must still be empty
        assertEquals(0, mFragment.getTracker().size());
    }

    @Test
    public void testGetUrlLoadTracker_returnsDefensiveCopy() {
        mFragment.trackUrl("https://login.microsoftonline.com/authorize", null, null);

        final Map<Integer, UrlStatus> copy1 = mFragment.getTracker();
        final Map<Integer, UrlStatus> copy2 = mFragment.getTracker();

        // Each call should return a distinct map object (defensive copy)
        assertNotSame(copy1, copy2);
    }
}
