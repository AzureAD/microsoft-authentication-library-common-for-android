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
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertTrue;
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
    public void testSelect_selectDefaultBrowser() throws NameNotFoundException {
        setBrowserList(CHROME, FIREFOX);
        when(mContext.getPackageManager().resolveActivity(BROWSER_INTENT, 0))
                .thenReturn(CHROME.mResolveInfo);
        List<Browser> allBrowsers = BrowserSelector.getAllBrowsers(mContext);
        assertTrue(allBrowsers.get(0).getPackageName().equals(CHROME.mPackageName));
        assertTrue(allBrowsers.get(1).getPackageName().equals(FIREFOX.mPackageName));
    }

    /**
     * Browsers are expected to be in priority order, such that the default would be first.
     */
    private void setBrowserList(TestBrowser... browsers) throws NameNotFoundException {
        if (browsers == null) {
            return;
        }

        List<ResolveInfo> resolveInfos = new ArrayList<>();

        for (TestBrowser browser : browsers) {
            when(mPackageManager.getPackageInfo(
                    eq(browser.mPackageInfo.packageName),
                    eq(PackageManager.GET_SIGNATURES)))
                    .thenReturn(browser.mPackageInfo);
            resolveInfos.add(browser.mResolveInfo);
        }

        when(mPackageManager.queryIntentActivities(
                BROWSER_INTENT,
                PackageManager.GET_RESOLVED_FILTER))
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

            for (String authority: mAuthorities) {
                ri.filter.addDataAuthority(authority, null);
            }

            return new TestBrowser(mPackageName, pi, ri, signatureHashes);
        }
    }
}
