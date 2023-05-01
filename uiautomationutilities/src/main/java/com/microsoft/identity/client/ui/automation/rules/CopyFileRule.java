package com.microsoft.identity.client.ui.automation.rules;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
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

public class CopyFileRule implements TestRule {
    private final static String TAG = CopyFileRule.class.getSimpleName();

    private String mSourceFolder;
    private String mDestFolder;
    private final String[] mApkFileNames = {
            BrokerHost.BROKER_HOST_APK,
            BrokerHost.OLD_BROKER_HOST_APK,
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
            BrokerLTW.OLD_BROKER_LTW_APK
    };

    public CopyFileRule() {
        mSourceFolder = "/sdcard/";
        mDestFolder = "/data/local/tmp/";
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

                for (String apkFileName: mApkFileNames){
                    AdbShellUtils.copyFile(mSourceFolder + apkFileName, mDestFolder);
                }

                base.evaluate();
            }
        };
    }
}
