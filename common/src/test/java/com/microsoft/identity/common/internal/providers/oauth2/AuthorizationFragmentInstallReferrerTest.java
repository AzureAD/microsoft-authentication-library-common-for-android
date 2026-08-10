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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.microsoft.identity.common.java.providers.MamCaRedirect;
import com.microsoft.identity.common.java.providers.MamInstallReferrerBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link AuthorizationFragment#decorateInstallLinkWithReferrer(String, Map)}, the shared
 * hook the browser/custom-tab authorization fragments use to tag a MAM Conditional Access Company
 * Portal install with the calling app as the Play install referrer.
 */
@RunWith(RobolectricTestRunner.class)
public class AuthorizationFragmentInstallReferrerTest {

    private static final String CALLING_PACKAGE = "com.contoso.callingapp";
    private static final String CP_APP_LINK =
            "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal";

    /**
     * Minimal concrete subclass exposing the protected hook, and letting a test decide what
     * {@code getContext()} returns without standing up a Fragment lifecycle.
     */
    private static class TestAuthorizationFragment extends AuthorizationFragment {

        private Context mTestContext;

        void setTestContext(final Context context) {
            mTestContext = context;
        }

        @Override
        public Context getContext() {
            return mTestContext;
        }

        String decorate(final String appLink, final Map<String, String> redirectParameters) {
            return decorateInstallLinkWithReferrer(appLink, redirectParameters);
        }
    }

    /**
     * @return a fragment whose context reports {@link #CALLING_PACKAGE} as its package name, with
     * the host opt-in already applied.
     */
    private TestAuthorizationFragment fragmentWithPackage(final String packageName) {
        final TestAuthorizationFragment fragment = new TestAuthorizationFragment();
        fragment.mMamCaInstallReferrerEnabled = true;
        if (packageName != null) {
            final Context context = Mockito.mock(Context.class);
            when(context.getPackageName()).thenReturn(packageName);
            fragment.setTestContext(context);
        }
        return fragment;
    }

    /**
     * @return broker-install redirect parameters, marked as MAM-CA when asked.
     */
    private Map<String, String> redirectParameters(final boolean mamCaMarked) {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(MamCaRedirect.KEY_USERNAME, "someuser@contoso.onmicrosoft.com");
        if (mamCaMarked) {
            parameters.put(MamCaRedirect.KEY_INTUNE_APP_PROTECTION,
                    MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED);
        }
        return parameters;
    }

    @Test
    public void testDecorate_mamCaInstall_appendsCallingPackageAsReferrer() {
        final String decorated = fragmentWithPackage(CALLING_PACKAGE)
                .decorate(CP_APP_LINK, redirectParameters(true));

        assertTrue("A MAM-CA install must name the calling app as the install referrer, but was: "
                        + decorated,
                decorated.contains(
                        MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + CALLING_PACKAGE));
    }

    @Test
    public void testDecorate_hostDidNotOptIn_returnsLinkUnchanged() {
        final TestAuthorizationFragment fragment = fragmentWithPackage(CALLING_PACKAGE);
        fragment.mMamCaInstallReferrerEnabled = false;

        final String decorated = fragment.decorate(CP_APP_LINK, redirectParameters(true));

        assertEquals("The host opt-in is a kill switch: the link must be untouched",
                CP_APP_LINK, decorated);
    }

    @Test
    public void testDecorate_notMamCaInstall_returnsLinkUnchanged() {
        final String decorated = fragmentWithPackage(CALLING_PACKAGE)
                .decorate(CP_APP_LINK, redirectParameters(false));

        assertEquals("An ordinary device-registration install must not be tagged",
                CP_APP_LINK, decorated);
    }

    /**
     * A detached fragment has no context, so there is no package to name. The install link must
     * still be returned so the install itself is never broken by referrer decoration.
     */
    @Test
    public void testDecorate_noContext_returnsLinkUnchanged() {
        final String decorated = fragmentWithPackage(null)
                .decorate(CP_APP_LINK, redirectParameters(true));

        assertEquals("Without a context there is no package to name, so the link is unchanged",
                CP_APP_LINK, decorated);
    }

    @Test
    public void testDecorate_nullAppLink_isTolerated() {
        assertNull(fragmentWithPackage(CALLING_PACKAGE).decorate(null, redirectParameters(true)));
    }

    @Test
    public void testDecorate_nullRedirectParameters_returnsLinkUnchanged() {
        assertEquals(CP_APP_LINK, fragmentWithPackage(CALLING_PACKAGE).decorate(CP_APP_LINK, null));
    }
}
