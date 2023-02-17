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
import androidx.annotation.Nullable;

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

    // Added to avoid "avoidduplicateliterals" issues in pmd.
    private static final String LOGGING_MSG_BROWSER = "Browser: ";

    /**
     * Searches through all browsers for the best match.
     * Browsers are evaluated in the order returned by the package manager,
     * which should indirectly match the user's preferences.
     * First matched browser in the list will be preferred no matter weather or not the custom tabs supported.
     *
     * @param context {@link Context} to use for accessing {@link PackageManager}.
     * @return Browser selected to use.
     */
    public static Browser select(@NonNull final Context context,
                                 @NonNull final List<BrowserDescriptor> browserSafeList,
                                 @Nullable final BrowserDescriptor preferredBrowserDescriptor) throws ClientException {
        final String methodTag = TAG + ":select";
        Logger.verbose(methodTag, "Select the browser to launch.");

        if (preferredBrowserDescriptor != null){
            final Browser preferredBrowser = getPreferredBrowser(context, preferredBrowserDescriptor);
            if (preferredBrowser != null) {
                return preferredBrowser;
            }
        }

        final Browser defaultBrowser = getDefaultBrowser(context, browserSafeList);
        if (defaultBrowser != null) {
            return defaultBrowser;
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
            Logger.warn(methodTag,LOGGING_MSG_BROWSER + browser.getPackageName() + " signature hash not match");
            return false;
        }

        if (!StringUtil.isNullOrEmpty(descriptor.getVersionLowerBound())
                && compareSemanticVersion(browser.getVersion(), descriptor.getVersionLowerBound()) == -1) {
            Logger.warn(methodTag,LOGGING_MSG_BROWSER + browser.getPackageName() +
                    " version too low (Expected: " + descriptor.getVersionLowerBound() +
                    " Get: " + browser.getVersion() + ")");
            return false;
        }

        if (!StringUtil.isNullOrEmpty(descriptor.getVersionUpperBound())
                && compareSemanticVersion(browser.getVersion(), descriptor.getVersionUpperBound()) == 1) {
            Logger.warn(methodTag,LOGGING_MSG_BROWSER + browser.getPackageName() +
                    " version too high (Expected: " + descriptor.getVersionUpperBound() +
                    " Get: " + browser.getVersion() + ")");
            return false;
        }

        return true;
    }

    private static Browser getPreferredBrowser(@NonNull final Context context,
                                              @NonNull final BrowserDescriptor preferredBrowserDescriptor){
        final String methodTag = TAG + ":getPreferredBrowser";

        final List<Browser> allBrowsers = getBrowsers(context, preferredBrowserDescriptor);
        for (final Browser browser : allBrowsers) {
            if (matches(preferredBrowserDescriptor, browser)) {
                Logger.info(
                        methodTag,
                        "Preferred Browser's package name: "
                                + browser.getPackageName()
                                + " version: "
                                + browser.getVersion());
                return browser;
            }
        }

        return null;
    }

    private static Browser getDefaultBrowser(@NonNull final Context context,
                                             @NonNull final List<BrowserDescriptor> browserSafeList) {
        final String methodTag = TAG + ":getDefaultBrowser";

        final List<Browser> allBrowsers = getBrowsers(context, null);
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

        return null;
    }


    /**
     * Retrieves the full list of browsers installed on the device.
     * If the browser supports custom tabs, it will {@link Browser#isCustomTabsServiceSupported()}
     * flag set to `true` in one and `false` in the other. The list is in the
     * order returned by the package manager, so indirectly reflects the user's preferences
     * (i.e. their default browser, if set, should be the first entry in the list).
     */
    protected static List<Browser> getBrowsers(@NonNull final Context context,
                                               @Nullable final BrowserDescriptor preferredBrowserDescriptor) {
        final String methodTag = TAG + ":getBrowsers";

        //get the list of browsers
        final Intent BROWSER_INTENT = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://www.example.com"));

        if (preferredBrowserDescriptor != null){
            Logger.info(methodTag, "Querying preferred browser: " + preferredBrowserDescriptor.getPackageName());
            BROWSER_INTENT.setPackage(preferredBrowserDescriptor.getPackageName());
        }

        final PackageManager pm = context.getPackageManager();

        int queryFlag = PackageManager.GET_RESOLVED_FILTER;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            queryFlag |= PackageManager.MATCH_DEFAULT_ONLY;
        }

        final List<ResolveInfo> resolvedActivityList =
                pm.queryIntentActivities(BROWSER_INTENT, queryFlag);

        Logger.verbose(methodTag, "Querying browsers. Got back " + resolvedActivityList.size() + " browsers.");

        final List<Browser> browserList = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            // ignore handlers which are not browsers
            if (!isFullBrowser(info)) {
                Logger.verbose(methodTag,LOGGING_MSG_BROWSER + info.activityInfo.packageName + " is not a full browser app.");
                continue;
            }

            try {
                final PackageInfo packageInfo = PackageHelper.getPackageInfo(pm, info.activityInfo.packageName);
                //TODO if the browser is in the block list, do not add it into the return browserList.
                if (isCustomTabsServiceSupported(context, packageInfo)) {
                    //if the browser has custom tab enabled, set the custom tab support as true.
                    Logger.verbose(methodTag,LOGGING_MSG_BROWSER + info.activityInfo.packageName + " supports custom tab.");
                    browserList.add(new Browser(packageInfo, true));
                } else {
                    Logger.verbose(methodTag,LOGGING_MSG_BROWSER + info.activityInfo.packageName + " does NOT support custom tab.");
                    browserList.add(new Browser(packageInfo, false));
                }
            } catch (PackageManager.NameNotFoundException e) {
                // a browser cannot be generated without the package info
                Logger.warn(methodTag,LOGGING_MSG_BROWSER + info.activityInfo.packageName + " cannot be generated without the package info.");
            }
        }

        Logger.verbose(methodTag, null, "Found " + browserList.size() + " browsers.");
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
