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
package com.microsoft.identity.client.ui.automation.broker;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsPromptHandler;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

import lombok.Getter;

/**
 * Serves as the base class interacting with the Microsoft Authenticator Broker App during UI Test. The base class should be extended
 * by BrokerAuthenticatorUpdatedVersionImpl, BrokerAuthenticatorPreviousVersionImpl
 */
@Getter
public class BrokerMicrosoftAuthenticator extends AbstractTestBroker implements ITestBroker, IPowerLiftIntegratedApp {

    public final static String AUTHENTICATOR_APP_PACKAGE_NAME = "com.azure.authenticator";
    public final static String AUTHENTICATOR_APP_NAME = "Microsoft Authenticator";
    public final static String AUTHENTICATOR_APK = "Authenticator.apk";
    public final static String OLD_AUTHENTICATOR_APK = "OldAuthenticator.apk";
    public final static boolean AUTHENTICATOR_IS_REGISTER_EXPECTED = true;
    public final static boolean AUTHENTICATOR_IS_REGISTER_EXPECTED_SHARED = false;

    private final static String UPDATE_VERSION_NUMBER = "6.2204.2470";
    private final static String OLD_VERSION_NUMBER = "6.2203.1651";

    private final static String INCIDENT_MSG = "Broker Automation Incident";

    private static final String TAG = BrokerMicrosoftAuthenticator.class.getSimpleName();

    protected boolean isInSharedDeviceMode = false;

    private BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticatorImpl;


    public BrokerMicrosoftAuthenticator() {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME);
        localApkFileName = AUTHENTICATOR_APK;
    }

    public BrokerMicrosoftAuthenticator(@NonNull final IAppInstaller appInstaller) {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME, appInstaller);
        localApkFileName = AUTHENTICATOR_APK;
    }

    public BrokerMicrosoftAuthenticator(@NonNull final IAppInstaller appInstaller, @NonNull final IAppInstaller updateAppInstaller) {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME, appInstaller, updateAppInstaller);
        localApkFileName = AUTHENTICATOR_APK;
    }

    public BrokerMicrosoftAuthenticator(@NonNull final String authenticatorApkName,
                                        @NonNull final String updateAuthenticatorApkName) {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME);
        localApkFileName = authenticatorApkName;
        localUpdateApkFileName = updateAuthenticatorApkName;
    }

    /**
     * Overriding the launch function to add a check for the app lock screen
     */
    @Override
    public void launch() {
        super.launch();
        handleAppLock();
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        performDeviceRegistration(username, password, false);
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password,
                                          final boolean isFederatedUser) {
        brokerMicrosoftAuthenticatorImpl.performDeviceRegistration(username, password, isFederatedUser);

        // This value was not being updated from the above performSharedDeviceRegistration method since
        // brokerMicrosoftAuthenticatorImpl is actually a completely separate object.
        shouldHandleFirstRun = brokerMicrosoftAuthenticatorImpl.shouldHandleFirstRun;
    }

    @Override
    public void performSharedDeviceRegistration(@NonNull final String username,
                                                @NonNull final String password) {
        brokerMicrosoftAuthenticatorImpl.performSharedDeviceRegistration(username, password);

        // These values were not being updated from the above performSharedDeviceRegistration method since
        // brokerMicrosoftAuthenticatorImpl is actually a completely separate object.
        isInSharedDeviceMode = brokerMicrosoftAuthenticatorImpl.isInSharedDeviceMode;
        shouldHandleFirstRun = brokerMicrosoftAuthenticatorImpl.shouldHandleFirstRun;
    }

    @Override
    public void performSharedDeviceRegistrationDontValidate(@NonNull final String username,
                                                @NonNull final String password) {
        brokerMicrosoftAuthenticatorImpl.performSharedDeviceRegistrationDontValidate(username, password);
    }


    @Nullable
    @Override
    public String obtainDeviceId() {
        Logger.i(TAG, "Obtain Device Id..");
        openDeviceRegistrationPage();

        try {
            final UiObject deviceIdElement = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.azure.authenticator:id/device_id_text"
            );

            final String deviceIdText = deviceIdElement.getText();
            final int colonIndex = deviceIdText.indexOf(":");
            return deviceIdText.substring(colonIndex + 1);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void enableBrowserAccess() {
        brokerMicrosoftAuthenticatorImpl.enableBrowserAccess();

        // This value was not being updated from the above performSharedDeviceRegistration method since
        // brokerMicrosoftAuthenticatorImpl is actually a completely separate object.
        shouldHandleFirstRun = brokerMicrosoftAuthenticatorImpl.shouldHandleFirstRun;
    }

    @Override
    public String createPowerLiftIncident() {
        Logger.i(TAG, "Creating Power Lift Incident..");
        launch();
        handleFirstRun();

        if (isInSharedDeviceMode) {
            return createPowerLiftIncidentInSharedDeviceMode();
        } else {
            return createPowerLiftIncidentInNonSharedMode();
        }
    }

    private String createPowerLiftIncidentInNonSharedMode() {
        // click the 3 dot menu icon in top right
        UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/menu_overflow");

        try {
            // select Help from drop down
            final UiObject settings = UiAutomatorUtils.obtainUiObjectWithText("Send Feedback");
            settings.click();

            final UiObject sendLogs = UiAutomatorUtils.obtainUiObjectWithClassAndDescription(
                    Button.class,
                    "Having trouble?Report it"
            );

            Assert.assertTrue(sendLogs.exists());

            // click the send logs button
            sendLogs.click();

            UiAutomatorUtils.handleButtonClickForObjectWithText("Select an option");

            UiAutomatorUtils.handleButtonClickForObjectWithText("Other");

            final UiObject describeIssueBox = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                    "Please don't include your name, phone number, or other personal information.",
                    EditText.class
            );

            describeIssueBox.setText(INCIDENT_MSG);

            final UiObject sendBtn = UiAutomatorUtils.obtainUiObjectWithDescription("Send feedback");
            sendBtn.click();

            final UiObject postLogSubmissionMsg = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "android:id/parentPanel"
            );

            Assert.assertTrue(postLogSubmissionMsg.exists());

            final UiObject incidentDetails = UiAutomatorUtils.obtainUiObjectWithResourceId("android:id/message");
            Assert.assertTrue(incidentDetails.exists());

            final String incidentIdText = incidentDetails.getText();

            // This will post the incident id in text logs
            Logger.w(TAG, incidentIdText);

            return incidentIdText;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private String createPowerLiftIncidentInSharedDeviceMode() {
        try {
            final UiObject settingsBtn = UiAutomatorUtils.obtainUiObjectWithClassAndDescription(
                    Button.class,
                    "Settings"
            );
            settingsBtn.click();

            UiAutomatorUtils.handleButtonClickForObjectWithText("Send logs");
            UiAutomatorUtils.handleInput(
                    "com.azure.authenticator:id/send_feedback_message_input", INCIDENT_MSG

            );
            UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/send_feedback_button");
            final UiObject postLogSubmissionText = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.azure.authenticator:id/send_feedback_result"
            );

            Assert.assertTrue(postLogSubmissionText.exists());

            final String incidentIdText = postLogSubmissionText.getText();
            // This will post the incident id in text logs
            Logger.w(TAG, incidentIdText);

            return incidentIdText;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public DeviceAdmin getAdminName() {
        Logger.i(TAG, "Get Admin Name..");
        return DeviceAdmin.MICROSOFT_AUTHENTICATOR;
    }

    /**
     * Open the device registration page in the Authenticator App
     */
    public void openDeviceRegistrationPage() {
        Logger.i(TAG, "Open the device registration page in the Authenticator App..");
        launch(); // launch Authenticator app

        handleFirstRun(); // handle first run experience
        goToDeviceRegistrationPage();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // grant the GET ACCOUNTS permission if needed
            grantPermission(Manifest.permission.GET_ACCOUNTS);
        }
    }

    protected void goToDeviceRegistrationPage() {
        brokerMicrosoftAuthenticatorImpl.goToDeviceRegistrationPage();
    }

    protected void performDeviceRegistrationHelper(@NonNull final String username,
                                                   @NonNull final String password,
                                                   @NonNull final String emailInputResourceId,
                                                   @NonNull final String registerBtnResourceId,
                                                   final boolean isFederatedUser,
                                                   final boolean isRegistrationPageExpected) {
        Logger.i(TAG, "Execution of Helper for Device Registration..");
        // open device registration page
        openDeviceRegistrationPage();

        // enter email
        UiAutomatorUtils.handleInput(
                emailInputResourceId,
                username
        );

        // click register
        UiAutomatorUtils.handleButtonClick(registerBtnResourceId);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .registerPageExpected(isRegistrationPageExpected)
                .loginHint(username)
                .build();

        if (isFederatedUser) {
            final AdfsPromptHandler adfsPromptHandler = new AdfsPromptHandler(promptHandlerParameters);
            Logger.i(TAG, "Handle prompt of ADFS login page for Device Registration..");
            // handle ADFS login page
            adfsPromptHandler.handlePrompt(username, password);
        } else {
            final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

            Logger.i(TAG, "Handle AAD Login page prompt for Device Registration..");
            // handle AAD login page
            aadPromptHandler.handlePrompt(username, password);
        }
    }

    public void setShouldUseDeviceSettingsPage(final boolean shouldUseDeviceSettingsPage) {
        Assert.assertTrue("Cannot set shouldUseDeviceSettingsPage for BrokerAuthenticatorPreviousVersionImpl", brokerMicrosoftAuthenticatorImpl instanceof BrokerAuthenticatorUpdatedVersionImpl);
        ((BrokerAuthenticatorUpdatedVersionImpl) brokerMicrosoftAuthenticatorImpl).shouldUseDeviceSettingsPage = shouldUseDeviceSettingsPage;
    }

    @Override
    public void handleFirstRun() {
        if (shouldHandleFirstRun) {
            Logger.i(TAG, "Handle First Run of the APP..");
            // privacy dialog
            UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/privacy_consent_button");
            // Continue button
            UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Continue");
            // the skip button
            UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/frx_skip_button");
            shouldHandleFirstRun = false;
        }
    }

    @Override
    protected void initialiseAppImpl() {
        final Context context = ApplicationProvider.getApplicationContext();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.azure.authenticator", 0);
            // authenticator app follows version number format - V.YYMM.XXXX
            // V is the major version, YYMM are last two digits of an year followed by month
            // XXXX is number of hours passed after Jan1st 00.00 of current year
            // String comparison of versions should work for this format
            final String authenticatorAppVersion = packageInfo.versionName;
            Logger.i(TAG, "Version of Authenticator app installed is " + authenticatorAppVersion);
            if (authenticatorAppVersion.compareTo(UPDATE_VERSION_NUMBER) >= 0) {
                // Use latest automation code as this is the new updated authenticator app
                brokerMicrosoftAuthenticatorImpl = new BrokerAuthenticatorUpdatedVersionImpl();
                Logger.i(TAG, "Using updated implementation of authenticator");
            } else if (authenticatorAppVersion.compareTo(OLD_VERSION_NUMBER) <= 0) {
                brokerMicrosoftAuthenticatorImpl = new BrokerAuthenticatorPreviousVersionImpl();
                Logger.i(TAG, "Using previous implementation of authenticator");
            } else {
                Assert.fail("Authenticator app version is not supported");
            }
        } catch (final PackageManager.NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public void handleAppLock() {
        Logger.i(TAG, "Checking for app lock popup on authenticator...");
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        final UiObject appLockObj = device.findObject(
                new UiSelector().text("App Lock enabled")
        );

        if (appLockObj.waitForExists(TimeUnit.SECONDS.toMillis(1))){
            final UiObject okObj = device.findObject(
                    new UiSelector().text("OK")
            );

            try {
                okObj.click();
            } catch (UiObjectNotFoundException e) {
                // Nothing, just want to ignore this
                // If the button does not exist, then we do not need to do anything
            }
        }
    }
}
