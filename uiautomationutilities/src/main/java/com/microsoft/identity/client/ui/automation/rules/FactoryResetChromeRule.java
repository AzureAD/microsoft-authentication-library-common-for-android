/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

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
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // validate Chrome version is suitable for automation
                validatePackageSuitableForAutomation(BrowserChrome.CHROME_PACKAGE_NAME, CHROME_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION);

                // validate WebView version is suitable for automation
                validatePackageSuitableForAutomation(WEB_VIEW_PACKAGE_NAME, WEB_VIEW_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION);

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
            Logger.v(TAG, packageName + " major version = " + majorVersion);
            return majorVersion;
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Package " + packageName + " not found :(", e);
            // we could probably install the package from PlayStore (but for now let's fail the test
            // to see if this ever happens in the wild)
            throw new AssertionError(e);
        }
    }

    private void validatePackageSuitableForAutomation(final String packageName,
                                                      final int majorVersionSuitableForAutomation) {
        final String packageVersion = getPackageMajorVersion(packageName);
        if (!TextUtils.isEmpty(packageVersion) &&
                Integer.parseInt(packageVersion) > majorVersionSuitableForAutomation) {
            Logger.w(TAG, packageName + " version on the device is higher than the known version suitable for automation. " +
                    "We are going to attempt to factory reset this package and hope that it will give us the desired version.");
            // adb uninstall does factory reset for default apps
            AdbShellUtils.removePackage(packageName);

            final String packageVersionAfterDowngrade = getPackageMajorVersion(packageName);
            Logger.i(TAG, packageName + " version after factory reset = " + packageVersionAfterDowngrade);

            if (Integer.parseInt(packageVersionAfterDowngrade) > majorVersionSuitableForAutomation) {
                Logger.w(TAG, packageName + " version after factory reset is still higher " +
                        "than the currently supported version: " + majorVersionSuitableForAutomation +
                        " - We are just going to fail the test at this point.");
                throw new AssertionError("Unsupported version " + packageVersionAfterDowngrade + " for package: " + packageName);
            }
        }
    }
}
