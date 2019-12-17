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
package com.microsoft.identity.common;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BrowserSelectorTest {
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    static final Intent BROWSER_INTENT = new Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://www.example.com"));

    private static final TestBrowser CHROME =
            new TestBrowserBuilder("com.android.chrome")
                    .withBrowserDefaults()
                    .setVersion("50")
                    .addSignature("ChromeSignature")
                    .build();

    private static final TestBrowser FIREFOX =
            new TestBrowserBuilder("org.mozilla.firefox")
                    .withBrowserDefaults()
                    .setVersion("10")
                    .addSignature("FirefoxSignature")
                    .build();

    private static final TestBrowser FIREFOX_CUSTOM_TAB =
            new TestBrowserBuilder("org.mozilla.firefox")
                    .withBrowserDefaults()
                    .setVersion("57")
                    .addSignature("FirefoxSignature")
                    .build();

    private static final TestBrowser DOLPHIN =
            new TestBrowserBuilder("mobi.mgeek.TunnyBrowser")
                    .withBrowserDefaults()
                    .setVersion("1.4.1")
                    .addSignature("DolphinSignature")
                    .build();

    @Mock
    Context mContext;
    @Mock
    PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void testSelect_getAllBrowser() throws NameNotFoundException {
        setBrowserList(CHROME, FIREFOX);
        when(mContext.getPackageManager().resolveActivity(BROWSER_INTENT, 0))
                .thenReturn(CHROME.mResolveInfo);
        List<Browser> allBrowsers = BrowserSelector.getAllBrowsers(mContext);
        assert (allBrowsers.get(0).getPackageName().equals(CHROME.mPackageName));
        assert (allBrowsers.get(1).getPackageName().equals(FIREFOX.mPackageName));
    }

    @Test
    public void testSelect_noMatchingBrowser() throws NameNotFoundException {
        setBrowserList(CHROME, FIREFOX);
        when(mContext.getPackageManager().resolveActivity(BROWSER_INTENT, 0))
                .thenReturn(CHROME.mResolveInfo);
        final List<BrowserDescriptor> browserSafelist = new ArrayList<>();
        try {
            BrowserSelector.select(mContext, browserSafelist);
        } catch (final ClientException exception) {
            assertNotNull(exception);
            assert (exception.getErrorCode().equalsIgnoreCase(ErrorStrings.NO_AVAILABLE_BROWSER_FOUND));
        }
    }

    @Test
    public void testSelect_versionNotSupportedBrowser() throws NameNotFoundException {
        setBrowserList(CHROME, FIREFOX);
        when(mContext.getPackageManager().resolveActivity(BROWSER_INTENT, 0))
                .thenReturn(CHROME.mResolveInfo);
        List<BrowserDescriptor> browserSafelist = new ArrayList<>();
        browserSafelist.add(
                new BrowserDescriptor(
                        "com.android.chrome",
                        "ChromeSignature",
                        "51",
                        null)
        );

        try {
            BrowserSelector.select(mContext, browserSafelist);
        } catch (final ClientException exception) {
            assertNotNull(exception);
            assert (exception.getErrorCode().equalsIgnoreCase(ErrorStrings.NO_AVAILABLE_BROWSER_FOUND));
        }
    }

    /**
     * Browsers are expected to be in priority order, such that the default would be first.
     */
    private void setBrowserList(TestBrowser... browsers) throws NameNotFoundException {
        if (browsers == null) {
            return;
        }

        final List<ResolveInfo> resolveInfos = new ArrayList<>();

        for (TestBrowser browser : browsers) {
            when(mPackageManager.getPackageInfo(
                    eq(browser.mPackageInfo.packageName),
                    eq(PackageManager.GET_SIGNATURES)))
                    .thenReturn(browser.mPackageInfo);
            resolveInfos.add(browser.mResolveInfo);
        }

        int queryFlag = PackageManager.GET_RESOLVED_FILTER;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            queryFlag |= PackageManager.MATCH_DEFAULT_ONLY;
        }

        when(mPackageManager.queryIntentActivities(
                BROWSER_INTENT,
                queryFlag))
                .thenReturn(resolveInfos);
    }

    private static class TestBrowser {
        final String mPackageName;
        final ResolveInfo mResolveInfo;
        final PackageInfo mPackageInfo;
        final Set<String> mSignatureHashes;

        TestBrowser(
                String packageName,
                PackageInfo packageInfo,
                ResolveInfo resolveInfo,
                Set<String> signatureHashes) {
            mPackageName = packageName;
            mResolveInfo = resolveInfo;
            mPackageInfo = packageInfo;
            mSignatureHashes = signatureHashes;
        }
    }

    private static class TestBrowserBuilder {
        private final String mPackageName;
        private final List<byte[]> mSignatures = new ArrayList<>();
        private final List<String> mActions = new ArrayList<>();
        private final List<String> mCategories = new ArrayList<>();
        private final List<String> mSchemes = new ArrayList<>();
        private final List<String> mAuthorities = new ArrayList<>();
        private String mVersion;

        TestBrowserBuilder(String packageName) {
            mPackageName = packageName;
        }

        public TestBrowserBuilder withBrowserDefaults() {
            return addAction(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addScheme(SCHEME_HTTP)
                    .addScheme(SCHEME_HTTPS);
        }

        public TestBrowserBuilder addAction(String action) {
            mActions.add(action);
            return this;
        }

        public TestBrowserBuilder addCategory(String category) {
            mCategories.add(category);
            return this;
        }

        public TestBrowserBuilder addScheme(String scheme) {
            mSchemes.add(scheme);
            return this;
        }

        public TestBrowserBuilder addAuthority(String authority) {
            mAuthorities.add(authority);
            return this;
        }

        public TestBrowserBuilder addSignature(String signature) {
            mSignatures.add(signature.getBytes(Charset.forName("UTF-8")));
            return this;
        }

        public TestBrowserBuilder setVersion(String version) {
            mVersion = version;
            return this;
        }

        public TestBrowser build() {
            PackageInfo pi = new PackageInfo();
            pi.packageName = mPackageName;
            pi.versionName = mVersion;
            pi.signatures = new Signature[mSignatures.size()];

            for (int i = 0; i < mSignatures.size(); i++) {
                pi.signatures[i] = new Signature(mSignatures.get(i));
            }

            Set<String> signatureHashes = Browser.generateSignatureHashes(pi.signatures);

            ResolveInfo ri = new ResolveInfo();
            ri.activityInfo = new ActivityInfo();
            ri.activityInfo.packageName = mPackageName;
            ri.filter = new IntentFilter();

            for (String action : mActions) {
                ri.filter.addAction(action);
            }

            for (String category : mCategories) {
                ri.filter.addCategory(category);
            }

            for (String scheme : mSchemes) {
                ri.filter.addDataScheme(scheme);
            }

            for (String authority : mAuthorities) {
                ri.filter.addDataAuthority(authority, null);
            }

            return new TestBrowser(mPackageName, pi, ri, signatureHashes);
        }
    }
}
