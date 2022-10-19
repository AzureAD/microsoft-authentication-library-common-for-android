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
package com.microsoft.identity.common.internal.ui.browser;

import static com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil.compareSemanticVersion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.common.internal.broker.PackageHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BrowserSelector {
    private static final String TAG = BrowserSelector.class.getSimpleName();
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    /**
     * Searches through all browsers for the best match.
     * Browsers are evaluated in the order returned by the package manager,
     * which should indirectly match the user's preferences.
     * First matched browser in the list will be preferred no matter weather or not the custom tabs supported.
     *
     * @param context {@link Context} to use for accessing {@link PackageManager}.
     * @return Browser selected to use.
     */
    public static Browser select(final Context context, final List<BrowserDescriptor> browserSafeList) throws ClientException {
        final String methodTag = TAG + ":select";
        final List<Browser> allBrowsers = getAllBrowsers(context);
        Logger.info(methodTag, "Select the browser to launch from safeList size=" + browserSafeList.size());

        for (final Browser browser : allBrowsers) {
            for (final BrowserDescriptor browserDescriptor : browserSafeList) {
                if (matches(browserDescriptor, browser)) {
                    Logger.info(
                            methodTag,
                            "Browser's package name: "
                                    + browser.getPackageName()
                                    + " version: "
                                    + browser.getVersion());
                    return browser;
                }
            }
        }

        Logger.error(methodTag, "No available browser installed on the device.", null);
        throw new ClientException(ErrorStrings.NO_AVAILABLE_BROWSER_FOUND, "No available browser installed on the device.");
    }

    private static boolean matches(@NonNull final BrowserDescriptor descriptor,
                                   @NonNull Browser browser) {
        final String methodTag = TAG + ":matches";

        if (!StringUtil.equalsIgnoreCase(descriptor.getPackageName(), browser.getPackageName())) {
            return false;
        }

        if (!descriptor.getSignatureHashes().equals(browser.getSignatureHashes())) {
            Logger.info(methodTag, "Signature hash does not match. " +
                    "Expects: " + descriptor.getSignatureHashes() +
                    " Found: " + browser.getSignatureHashes());
            return false;
        }

        if (!StringUtil.isNullOrEmpty(descriptor.getVersionLowerBound())
                && compareSemanticVersion(browser.getVersion(), descriptor.getVersionLowerBound()) == -1) {
            Logger.info(methodTag, "Browser version too low. " +
                    "Min. supported version: " + descriptor.getVersionLowerBound() +
                    " Found: " + browser.getVersion());
            return false;
        }

        if (!StringUtil.isNullOrEmpty(descriptor.getVersionUpperBound())
                && compareSemanticVersion(browser.getVersion(), descriptor.getVersionUpperBound()) == 1) {
            Logger.info(methodTag, "Browser version too high. " +
                    "Max supported version: " + descriptor.getVersionLowerBound() +
                    " Found: " + browser.getVersion());
            return false;
        }

        return true;
    }

    /**
     * Retrieves the full list of browsers installed on the device.
     * If the browser supports custom tabs, it will {@link Browser#isCustomTabsServiceSupported()}
     * flag set to `true` in one and `false` in the other. The list is in the
     * order returned by the package manager, so indirectly reflects the user's preferences
     * (i.e. their default browser, if set, should be the first entry in the list).
     */
    public static List<Browser> getAllBrowsers(final Context context) {
        final String methodTag = TAG + ":getAllBrowsers";
        //get the list of browsers
        final Intent BROWSER_INTENT = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://www.example.com"));

        List<Browser> browserList = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        int queryFlag = PackageManager.GET_RESOLVED_FILTER;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            queryFlag |= PackageManager.MATCH_DEFAULT_ONLY;
        }

        List<ResolveInfo> resolvedActivityList =
                pm.queryIntentActivities(BROWSER_INTENT, queryFlag);

        for (ResolveInfo info : resolvedActivityList) {
            // ignore handlers which are not browsers
            if (!isFullBrowser(info)) {
                Logger.info(methodTag, null,
                        "Ignoring: " + info.activityInfo.packageName + " as it is not a full browser.");
                continue;
            }

            try {
                final PackageInfo packageInfo = PackageHelper.getPackageInfo(pm, info.activityInfo.packageName);
                //TODO if the browser is in the block list, do not add it into the return browserList.
                if (isCustomTabsServiceSupported(context, packageInfo)) {
                    //if the browser has custom tab enabled, set the custom tab support as true.
                    browserList.add(new Browser(packageInfo, true));
                } else {
                    browserList.add(new Browser(packageInfo, false));
                }
                Logger.info(methodTag, null, "Found supported browser: " + packageInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // a browser cannot be generated without the package info
                Logger.info(methodTag, null, "Package name not found: " + info.activityInfo.packageName);
            }
        }

        Logger.info(methodTag, null, "Found " + browserList.size() + " browsers.");
        return browserList;
    }

    private static boolean isCustomTabsServiceSupported(@NonNull final Context context, @NonNull final PackageInfo packageInfo) {
        // https://issuetracker.google.com/issues/119183822
        // When above AndroidX issue is fixed, switch back to CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
        Intent serviceIntent = new Intent(new StringBuilder("android").append(".support.customtabs.action.CustomTabsService").toString());
        serviceIntent.setPackage(packageInfo.packageName);
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentServices(serviceIntent, 0);
        return !(resolveInfos == null || resolveInfos.isEmpty());
    }

    private static boolean isFullBrowser(final ResolveInfo resolveInfo) {
        // The filter must match ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme,
        if (!resolveInfo.filter.hasAction(Intent.ACTION_VIEW)
                || !resolveInfo.filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                || resolveInfo.filter.schemesIterator() == null) {
            return false;
        }

        // The filter must not be restricted to any particular set of authorities
        if (resolveInfo.filter.authoritiesIterator() != null) {
            return false;
        }

        // The filter must support both HTTP and HTTPS.
        boolean supportsHttp = false;
        boolean supportsHttps = false;
        Iterator<String> schemeIter = resolveInfo.filter.schemesIterator();
        while (schemeIter.hasNext()) {
            String scheme = schemeIter.next();
            supportsHttp |= SCHEME_HTTP.equals(scheme);
            supportsHttps |= SCHEME_HTTPS.equals(scheme);

            if (supportsHttp && supportsHttps) {
                return true;
            }
        }

        // at least one of HTTP or HTTPS is not supported
        return false;
    }
}
