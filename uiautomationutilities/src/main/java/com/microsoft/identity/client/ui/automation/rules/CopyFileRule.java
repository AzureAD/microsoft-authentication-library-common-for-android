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
package com.microsoft.identity.client.ui.automation.rules;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OutlookApp;
import com.microsoft.identity.client.ui.automation.app.TeamsApp;
import com.microsoft.identity.client.ui.automation.app.WordApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.BrowserEdge;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * We use this rule to copy files from a particular directory in Android device to another directory,
 * in case firebase does not allow us to push to and install apks from the same directory.
 */
public class CopyFileRule implements TestRule {
    private final static String TAG = CopyFileRule.class.getSimpleName();

    private final static String DEFAULT_SOURCE_FOLDER = "/sdcard/";
    private final static String DEFAULT_DESTINATION_FOLDER = "/data/local/tmp/";

    // Lab Application certificate file
    private final static String LAB_VAULT_ACCESS_CERT_PFX = "LabVaultAccessCert.pfx";

    private final String mSourceFolder;
    private final String mDestFolder;
    private final String[] mApkFileNames = {
            BrokerHost.BROKER_HOST_APK,
            BrokerHost.OLD_BROKER_HOST_APK,
            BrokerHost.BROKER_HOST_WITHOUT_BROKER_SELECTION_APK,
            AzureSampleApp.AZURE_SAMPLE_APK,
            AzureSampleApp.OLD_AZURE_SAMPLE_APK,
            BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK,
            BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK,
            BrokerCompanyPortal.COMPANY_PORTAL_APK,
            BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
            TeamsApp.TEAMS_APK,
            OutlookApp.OUTLOOK_APK,
            WordApp.WORD_APK,
            BrowserEdge.EDGE_APK,
            BrokerLTW.BROKER_LTW_APK,
            BrokerLTW.OLD_BROKER_LTW_APK,
            OneAuthTestApp.ONEAUTH_TESTAPP_APK,
            OneAuthTestApp.OLD_ONEAUTH_TESTAPP_APK,
            MsalTestApp.MSAL_TEST_APP_APK,
            MsalTestApp.OLD_MSAL_TEST_APP_APK
    };

    public CopyFileRule() {
        mSourceFolder = DEFAULT_SOURCE_FOLDER;
        mDestFolder = DEFAULT_DESTINATION_FOLDER;
    }

    public CopyFileRule(@NonNull final String sourceFolder, @NonNull final String destFolder) {
        mSourceFolder = sourceFolder;
        mDestFolder = destFolder;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                Logger.i(TAG, "Copying into /data/local/tmp...");

                for (final String apkFileName: mApkFileNames){
                    AdbShellUtils.copyFile(mSourceFolder + apkFileName, mDestFolder);
                }

                AdbShellUtils.copyFile(mSourceFolder + LAB_VAULT_ACCESS_CERT_PFX, mDestFolder);

                base.evaluate();
            }
        };
    }
}
