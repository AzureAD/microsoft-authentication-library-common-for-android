package com.microsoft.identity.client.ui.automation.rules;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.launchIntent;

public class DevicePinSetupRule implements TestRule {

    final Boolean isLocked = IsDeviceSecured();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (!isLocked) {
                    setLock();
                }
                base.evaluate();
            }
        };
    }

    private boolean IsDeviceSecured() {
        Context context = ApplicationProvider.getApplicationContext();
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); //api 16+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        }
        return keyguardManager.isKeyguardSecure();
    }

    private void setLock() throws UiObjectNotFoundException {

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        launchIntent(Settings.ACTION_SECURITY_SETTINGS);

        UiObject screenLock = UiAutomatorUtils.obtainUiObjectWithText("Screen lock");
        Assert.assertTrue(screenLock.exists());
        screenLock.click();
        //click on PIN.
        UiObject PinButton = UiAutomatorUtils.obtainUiObjectWithExactText("PIN");
        Assert.assertTrue(PinButton.exists());
        PinButton.click();

        //click on NO
        UiObject noButton = UiAutomatorUtils.obtainUiObjectWithExactText("NO");
        Assert.assertTrue(noButton.exists());
        noButton.click();

        //press 1234
        UiObject passwordField = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.settings:id/password_entry");
        Assert.assertTrue(passwordField.exists());
        passwordField.setText("1234");
        //press Enter.
        //UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.pressEnter();
        //press
        passwordField = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.settings:id/password_entry");
        passwordField.setText("1234");
        device.pressEnter();
        //click on done button.
        UiObject doneButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.settings:id/redaction_done_button");
        Assert.assertTrue(doneButton.exists());
        doneButton.click();
    }
}
