//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.ui.automation.rules;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to downgrade Chrome and WebView to factory default version. The newer versions of
 * Chrome & WebView seem to be not so suitable for automation and we run into resource id not found
 * error. Basically it seems that Chrome rolled out an update due to which automation frameworks are
 * not able to find elements on the web-page (at least this is the case for ESTS UX page). To
 * mitigate this, if we find that Chrome or WebView version on the device is above the known version
 * suitable for automation, then we would factory reset Chrome.
 */
public class FactoryResetChromeRule implements TestRule {

    public static final String TAG = FactoryResetChromeRule.class.getSimpleName();

    private final static int CHROME_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION = 74;
    private final static int WEB_VIEW_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION = 74;
    private final static String WEB_VIEW_PACKAGE_NAME = "com.google.android.webview";

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final String chromeVersion = getPackageMajorVersion(BrowserChrome.CHROME_PACKAGE_NAME);
                if (!TextUtils.isEmpty(chromeVersion) &&
                        Integer.parseInt(chromeVersion) > CHROME_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION) {
                    Logger.w(TAG, "Chrome version on the device is higher than the known version suitable for automation. " +
                            "We are going to attempt to factory reset Chrome and hope that it will give us the desired version.");
                    // adb uninstall does factory reset for default apps
                    AdbShellUtils.removePackage(BrowserChrome.CHROME_PACKAGE_NAME);
                }

                final String webViewVersion = getPackageMajorVersion(WEB_VIEW_PACKAGE_NAME);
                if (!TextUtils.isEmpty(webViewVersion) &&
                        Integer.parseInt(webViewVersion) > WEB_VIEW_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION) {
                    Logger.w(TAG, "Chrome version on the device is higher than the known version suitable for automation. " +
                            "We are going to attempt to factory reset Chrome and hope that it will give us the desired version.");
                    // adb uninstall does factory reset for default apps
                    AdbShellUtils.removePackage(WEB_VIEW_PACKAGE_NAME);
                }

                // proceed with the test case
                base.evaluate();
            }
        };
    }

    private String getPackageMajorVersion(final String packageName) {
        try {
            final Context context = ApplicationProvider.getApplicationContext();
            final PackageManager packageManager = context.getPackageManager();
            final PackageInfo chromePackageInfo = packageManager.getPackageInfo(packageName, 0);
            final String chromeVersion = chromePackageInfo.versionName;
            final String[] parts = chromeVersion.split("\\.");
            final String majorVersion = parts[0];
            Logger.i(TAG, packageName + " Version = " + chromeVersion);
            Logger.i(TAG, packageName + " major version = " + majorVersion);
            return majorVersion;
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Package " + packageName + " not found :(", e);
            // we could probably install the package from PlayStore (but for now let's fail the test
            // to see if this ever happens in the wild)
            throw new AssertionError(e);
        }
    }
}
