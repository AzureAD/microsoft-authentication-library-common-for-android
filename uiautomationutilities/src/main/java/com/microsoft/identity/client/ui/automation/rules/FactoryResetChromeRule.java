package com.microsoft.identity.client.ui.automation.rules;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.device.TestDevice;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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
                    // adb uninstall does factory reset for default apps
                    AdbShellUtils.removePackage(BrowserChrome.CHROME_PACKAGE_NAME);
                }

                final String webViewVersion = getPackageMajorVersion(WEB_VIEW_PACKAGE_NAME);
                if (!TextUtils.isEmpty(webViewVersion) &&
                        Integer.parseInt(webViewVersion) > WEB_VIEW_MAJOR_VERSION_SUITABLE_FOR_AUTOMATION) {
                    // adb uninstall does factory reset for default apps
                    AdbShellUtils.removePackage(WEB_VIEW_PACKAGE_NAME);
                }

                // proceed with the test case
                base.evaluate();
            }
        };
    }

    private void downgradeChromeToFactoryVersion() {
        final TestDevice device = TestContext.getTestContext().getTestDevice();
        device.getSettings().launchAppInfoPage(BrowserChrome.CHROME_PACKAGE_NAME);

        // disable chrome
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/button1_negative");

        // confirm disable in dialog
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        // confirm downgrade to factory version
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        // Enable Chrome
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/button1_positive");
    }

    private String getPackageMajorVersion(final String packageName) {
        try {
            final Context context = ApplicationProvider.getApplicationContext();
            final PackageManager packageManager = context.getPackageManager();
            final PackageInfo chromePackageInfo = packageManager.getPackageInfo(packageName, 0);
            final String chromeVersion = chromePackageInfo.versionName;
            final String[] parts = chromeVersion.split("\\.");
            final String majorVersion = parts[0];
            Log.i(TAG, "Chrome Version = " + chromeVersion);
            Log.i(TAG, "Chrome major version = " + majorVersion);
            return majorVersion;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
