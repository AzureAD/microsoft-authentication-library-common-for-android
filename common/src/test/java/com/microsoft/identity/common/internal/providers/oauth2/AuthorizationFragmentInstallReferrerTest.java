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
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.MAM_CA_INSTALL_REFERRER_ENABLED;

import android.content.Context;
import android.os.Bundle;

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
        final TestAuthorizationFragment fragment = fragmentAwaitingState(packageName);
        fragment.mMamCaInstallReferrerEnabled = true;
        return fragment;
    }

    /**
     * @return a fragment with a package name but no host opt-in yet, so a test can supply it the
     * way the activity does - through {@link AuthorizationFragment#extractState(Bundle)}.
     */
    private TestAuthorizationFragment fragmentAwaitingState(final String packageName) {
        final TestAuthorizationFragment fragment = new TestAuthorizationFragment();
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

    /**
     * The opt-in reaches the fragment as an intent extra rather than being set on the field, so
     * cover that hop and the save/restore round trip it has to survive if the process is killed
     * mid-flow. The tests above assign the field directly and so would not notice if either the
     * read in {@link AuthorizationFragment#extractState(Bundle)} or the write in
     * {@link AuthorizationFragment#onSaveInstanceState(Bundle)} were removed.
     */
    @Test
    public void testExtractState_hostOptIn_reachesDecorationAndSurvivesSaveRestore() {
        final Bundle launchState = new Bundle();
        launchState.putBoolean(MAM_CA_INSTALL_REFERRER_ENABLED, true);

        final TestAuthorizationFragment fragment = fragmentAwaitingState(CALLING_PACKAGE);
        fragment.extractState(launchState);

        assertTrue("The opt-in must reach the decoration through the fragment state",
                fragment.decorate(CP_APP_LINK, redirectParameters(true))
                        .contains(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + CALLING_PACKAGE));

        final Bundle savedState = new Bundle();
        fragment.onSaveInstanceState(savedState);

        final TestAuthorizationFragment restored = fragmentAwaitingState(CALLING_PACKAGE);
        restored.extractState(savedState);

        assertTrue("The opt-in must survive being saved and restored across process death",
                restored.decorate(CP_APP_LINK, redirectParameters(true))
                        .contains(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + CALLING_PACKAGE));
    }

    /**
     * MSAL and the broker never set the extra, so an absent key has to read as off.
     */
    @Test
    public void testExtractState_optInAbsentFromState_returnsLinkUnchanged() {
        final TestAuthorizationFragment fragment = fragmentAwaitingState(CALLING_PACKAGE);
        fragment.extractState(new Bundle());

        assertEquals("A host that does not set the extra must be left alone",
                CP_APP_LINK, fragment.decorate(CP_APP_LINK, redirectParameters(true)));
    }
}
