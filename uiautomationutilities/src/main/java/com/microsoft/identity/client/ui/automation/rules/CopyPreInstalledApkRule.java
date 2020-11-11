package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CopyPreInstalledApkRule implements TestRule {

    private final String TAG = CopyPreInstalledApkRule.class.getSimpleName();

    private List<IApp> mPreInstalledAppsToCopy;

    public CopyPreInstalledApkRule(final List<IApp> preInstalledAppsToCopy) {
        mPreInstalledAppsToCopy = preInstalledAppsToCopy;
    }

    public CopyPreInstalledApkRule(final IApp... preInstalledAppsToCopy) {
        mPreInstalledAppsToCopy = new ArrayList<>();
        mPreInstalledAppsToCopy.addAll(Arrays.asList(preInstalledAppsToCopy));
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "Applying rule....");
                for (final IApp app : mPreInstalledAppsToCopy) {
                    Log.i(TAG, "Attempting to copy APK for " + ((App) app).getAppName());
                    if (app.isInstalled()) {
                        Log.i(TAG, "Detected pre-installed app: " + ((App) app).getAppName());
                        Log.i(TAG, "Proceeding with copying apk for: " + ((App) app).getAppName());
                        app.copyApk(getDestApkFileName(app));
                    } else {
                        Log.i(TAG, "Can't copy APK for: " + ((App) app).getAppName() + " as it is not pre-installed");
                    }
                }

                base.evaluate();
            }
        };
    }

    private String getDestApkFileName(final IApp app) {
        if (((App) app).getLocalApkFileName() != null) {
            return LocalApkInstaller.LOCAL_APK_PATH_PREFIX + ((App) app).getLocalApkFileName();
        } else {
            return LocalApkInstaller.LOCAL_APK_PATH_PREFIX + app.getClass().getSimpleName();
        }
    }

}
