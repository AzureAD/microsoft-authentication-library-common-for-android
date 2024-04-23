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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.constants.GlobalConstants;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.device.settings.ISettings;
import com.microsoft.identity.client.ui.automation.device.settings.SamsungSettings;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

/**
 * A model for interacting with the Company Portal Broker App during UI Test.
 */
@Getter
public class BrokerCompanyPortal extends AbstractTestBroker implements ITestBroker, IMdmAgent, IPowerLiftIntegratedApp {

    public static final String TAG = BrokerCompanyPortal.class.getSimpleName();

    public final static String COMPANY_PORTAL_APP_PACKAGE_NAME = "com.microsoft.windowsintune.companyportal";
    public final static String COMPANY_PORTAL_APP_NAME = "Intune Company Portal";
    public final static String COMPANY_PORTAL_APK = "CompanyPortal.apk";
    public final static String OLD_COMPANY_PORTAL_APK = "OldCompanyPortal.apk";
    private final static int PASSWORD_UI_ATTEMPT_COUNT = 3;

    // Timeout to wait for complete enrollment page to appear
    final static long COMPLETE_ENROLLMENT_PAGE_TIMEOUT = TimeUnit.SECONDS.toMillis(45);

    private boolean enrollmentPerformedSuccessfully;
    private boolean batteryOptimizationTurnedOff;

    public BrokerCompanyPortal() {
        super(COMPANY_PORTAL_APP_PACKAGE_NAME, COMPANY_PORTAL_APP_NAME);
        localApkFileName = COMPANY_PORTAL_APK;
    }

    public BrokerCompanyPortal(@NonNull final IAppInstaller appInstaller) {
        super(COMPANY_PORTAL_APP_PACKAGE_NAME, COMPANY_PORTAL_APP_NAME, appInstaller);
        localApkFileName = COMPANY_PORTAL_APK;
    }

    public BrokerCompanyPortal(@NonNull final String companyPortalApkName,
                                        @NonNull final String updateCompanyPortalApkName) {
        super(COMPANY_PORTAL_APP_PACKAGE_NAME, COMPANY_PORTAL_APP_NAME);
        localApkFileName = companyPortalApkName;
        localUpdateApkFileName = updateCompanyPortalApkName;
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        Logger.i(TAG, "Perform Device Registration for the given account..");
        TestContext.getTestContext().getTestDevice().getSettings().addWorkAccount(
                this,
                username,
                password
        );
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password,
                                          final boolean isFederatedUser) {
        Logger.i(TAG, "Perform Device Registration for the given federate account..");
        TestContext.getTestContext().getTestDevice().getSettings().addWorkAccount(
                this,
                username,
                password,
                isFederatedUser
        );
    }

    @Override
    public void performSharedDeviceRegistration(@NonNull final String username,
                                                @NonNull final String password) {
        //TODO implement shared device registration for CP
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public void performSharedDeviceRegistrationDontValidate(@NonNull final String username,
                                                            @NonNull final String password) {
        throw new UnsupportedOperationException("Not Supported!");
    }

    @Nullable
    @Override
    public String obtainDeviceId() {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public void enableBrowserAccess() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String createPowerLiftIncident() {
        Logger.i(TAG, "Creating Power Lift Incident..");
        launch();
        handleFirstRun();

        try {
            final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // Click more options in the top right
            final UiObject threeDots = device.findObject(new UiSelector().descriptionContains(
                    "More options"
            ));

            threeDots.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

            threeDots.click();

            // Select Help from menu
            final UiObject helpBtn = UiAutomatorUtils.obtainUiObjectWithText("Help");

            helpBtn.click();

            // Click Email Support
            UiAutomatorUtils.handleButtonClick(
                    "com.microsoft.windowsintune.companyportal:id/email_support_subsection_title"
            );

            // Click Upload Logs Only
            UiAutomatorUtils.handleButtonClick(
                    "com.microsoft.windowsintune.companyportal:id/upload_button"
            );

            final UiObject incidentIdBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.windowsintune.companyportal:id/incident_id_subsection_description"
            );

            Assert.assertTrue(incidentIdBox.exists());

            final String incidentDetails = incidentIdBox.getText();

            Logger.w(TAG, "Incident Created with ID: " + incidentDetails);

            return incidentDetails;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public DeviceAdmin getAdminName() {
        return DeviceAdmin.COMPANY_PORTAL;
    }

    @Override
    public void handleFirstRun() {
        if (shouldHandleFirstRun) {
            // click the I AGREE btn on privacy screen
            // First run of CP from playstore does not have a privacy screen
            // UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/privacy_notice_agree_button");
            shouldHandleFirstRun = false;
        }
    }

    public void enrollDevice(@NonNull final String username,
                             @NonNull final String password,
                             final boolean isFederated){
        Logger.i(TAG, "Enroll Device for the given account..");
        launch(); // launch CP app

        handleFirstRun(); // handle CP first run

        signInThroughFrontPage(username, password, isFederated);

        // click the activate device admin btn
        final UiObject accessSetupScreen = UiAutomatorUtils.obtainUiObjectWithText("Access Setup");
        Assert.assertTrue(
                "CP Enrollment - Access Setup screen appears",
                accessSetupScreen.waitForExists(CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG)
        );

        // click on BEGIN button to start enroll
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/setup_positive_button");

        // click CONTINUE to ack privacy page
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/ContinueButton");

        // click NEXT to ack Android system permissions requirements
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/bullet_list_page_forward_button");

        // grant permission
        CommonUtils.grantPackagePermission();

        // Activate CP as admin
        TestContext.getTestContext().getTestDevice().getSettings().activateAdmin();

        final ISettings deviceSettings = TestContext.getTestContext().getTestDevice().getSettings();

        // if on a Samsung device, also need to handle enrollment in Knox
        if (deviceSettings instanceof SamsungSettings) {
            ((SamsungSettings) deviceSettings).enrollInKnox();
        }

        // make sure we are on the page to complete setup
        final UiObject setupCompletePage = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.windowsintune.companyportal:id/setup_title"
        );

        if (!setupCompletePage.waitForExists(COMPLETE_ENROLLMENT_PAGE_TIMEOUT)) {
            // Something went wrong with enrollment. If we see a device limit reached dialog, then
            // we throw a DeviceLimitReachedException so that we the DeviceEnrollmentRecoveryRule
            // can perform cleanup and recovery for future enrollments.
            final UiObject deviceLimitReachedDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.windowsintune.companyportal:id/alertTitle"
            );

            if (deviceLimitReachedDialog.exists()) {
                Logger.w(TAG, "Device limit reached for the given account..");
                throw new DeviceLimitReachedException(
                        "Unable to complete enrollment as device limit reached for this account.",
                        this
                );
            } else {
                // We don't see device limit issue, but the enrollment still failed due to reasons
                // that aren't immediately known
                Assert.fail("Unable to complete enrollment due to unknown reason");
            }
        }

        // click on DONE to complete setup
        UiAutomatorUtils.handleButtonClick(
                "com.microsoft.windowsintune.companyportal:id/setup_center_button"
        );

        // Enrollment has been performed successfully
        enrollmentPerformedSuccessfully = true;
    }

    @Override
    public void enrollDevice(@NonNull final String username,
                             @NonNull final String password) {
        enrollDevice(username, password, false);
    }

    private void signInThroughFrontPage(@NonNull final String username, @NonNull final String password, final boolean isFederated){
        // click Sign In button on CP welcome page
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/sign_in_button");

        if (isFederated) {
            final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                    .prompt(PromptParameter.LOGIN)
                    .consentPageExpected(false)
                    .expectingLoginPageAccountPicker(false)
                    .sessionExpected(false)
                    .isFederated(true)
                    .loginHint(null)
                    .build();

            final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);
            ((AdfsLoginComponentHandler) microsoftStsPromptHandler.getLoginComponentHandler()).handleEnrollmentPrompt(username, password);
        } else {
            final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                    .prompt(PromptParameter.LOGIN)
                    .broker(this)
                    .consentPageExpected(false)
                    .expectingLoginPageAccountPicker(false)
                    .sessionExpected(false)
                    .loginHint(null)
                    .build();

            final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);

            Logger.i(TAG, "Handle prompt in AAD login page for enrolling device..");
            // handle AAD login page
            microsoftStsPromptHandler.handlePrompt(username, password);
        }
    }

    /**
     * Method used to complete device enrollment with a Work Profile account. By the end of this automation,
     * the device should have work profile enabled.
     *
     * @param username username of the account
     * @param password password of the account
     */
    public void enrollDeviceForWorkProfile(@NonNull final String username,
                                           @NonNull final String password) {
        Logger.i(TAG, "Enroll Device for the given account..");
        launch(); // launch CP app

        handleFirstRun(); // handle CP first run

        // click Sign In button on CP welcome page
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/sign_in_button");

        final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .consentPageExpected(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(null)
                .build();

        final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler(promptHandlerParameters);

        Logger.i(TAG, "Handle prompt in AAD login page for enrolling device..");
        // handle AAD login page
        microsoftStsPromptHandler.handlePrompt(username, password);

        // click the activate device admin btn
        final UiObject accessSetupScreen = UiAutomatorUtils.obtainUiObjectWithText("Access Setup");
        Assert.assertTrue(
                "CP Enrollment - Access Setup screen appears",
                accessSetupScreen.exists()
        );

        // click on BEGIN button to start enroll
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/setup_positive_button");

        // click CONTINUE to ack privacy page
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/ContinueButton");

        UiAutomatorUtils.handleButtonClickForObjectWithText("Accept & continue");

        UiAutomatorUtils.handleButtonClickForObjectWithText("Next");

        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/setup_positive_button");

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(45));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // click on DONE to complete setup
        UiAutomatorUtils.handleButtonClick(
                "com.microsoft.windowsintune.companyportal:id/setup_center_button"
        );

        // Enrollment has been performed successfully
        enrollmentPerformedSuccessfully = true;
    }

    @Override
    public void handleAppProtectionPolicy() {
        Logger.i(TAG, "Handle App Protection Policy..");

        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // get access screen
        final UiObject getAccessScreen = UiAutomatorUtils.obtainUiObjectWithText("Get Access");
        Assert.assertTrue(
                "CP - Get Access screen appears",
                getAccessScreen.waitForExists(TimeUnit.MINUTES.toMillis(2))
        );

        // get access screen - continue
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/positive_button");

        // handle PIN
        Logger.i(TAG, "Handle PIN to enable App Protection Policy..");
        final UiObject pinField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.windowsintune.companyportal:id/pin_entry_passcodeEditView"
        );

        try {
            pinField.setText(GlobalConstants.PIN);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        device.pressEnter();

        // confirm PIN
        final UiObject pinConfirmField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.windowsintune.companyportal:id/pin_entry_passcodeEditView"
        );

        try {
            pinConfirmField.setText(GlobalConstants.PIN);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        device.pressEnter();
    }

    private void openDevicesTab() {
        Logger.i(TAG, "Open Devices Tab..");
        // launch CP
        launch();

        try {
            final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // Click Devices Tab
            final UiObject devicesTab = device.findObject(new UiSelector().description(
                    "Devices, Tab, 2 of 3"
            ).clickable(true));

            devicesTab.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

            devicesTab.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Removes a device from Company Portal (from the devices listed in CP Devices Tab)
     */
    public void removeDevice() {
        Logger.i(TAG, "Removes a device from Company Portal..");
        // if enrollment failed, then Devices Tab is automatically opened for us
        if (enrollmentPerformedSuccessfully) {
            openDevicesTab();
        }

        try {
            final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // If Enrollment failed, then the first device on the list is in corrupted state and
            // cannot even be removed, we need to remove the second one in the list
            final UiObject deviceToRemove = uiDevice.findObject(new UiSelector()
                    .resourceId("com.microsoft.windowsintune.companyportal:id/device_list_item")
                    .index(enrollmentPerformedSuccessfully ? 0 : 1)
            );

            deviceToRemove.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

            // click on the device to be removed
            deviceToRemove.click();

            // Click more options in the top right
            final UiObject threeDots = uiDevice.findObject(new UiSelector().descriptionContains(
                    "More options"
            ));

            threeDots.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

            threeDots.click();

            // Select Remove from menu
            final UiObject removeBtn = UiAutomatorUtils.obtainUiObjectWithText("Remove");

            removeBtn.click();

            final UiObject removeDeviceDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.windowsintune.companyportal:id/alertTitle"
            );

            Assert.assertTrue(
                    "CP Remove device dialog appears.",
                    removeDeviceDialog.exists()
            );

            // Confirm removal
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected void initialiseAppImpl() {
       // nothing needed here
    }

    public void turnOffBatteryOptimization() {
        if (!batteryOptimizationTurnedOff) {
            Logger.i(TAG, "Turning Off battery optimization...");

            try {
                final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

                // Click more options in the top right
                final UiObject threeDots = device.findObject(new UiSelector().descriptionContains(
                        "More options"
                ));
                threeDots.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
                threeDots.click();

                // Select Settings from menu
                final UiObject settingsBtn = UiAutomatorUtils.obtainUiObjectWithText("Settings");
                settingsBtn.click();

                // Click TURN OFF Button (Battery Optimization)
                final UiObject turnOffBtn = device.findObject(
                        new UiSelector().text("TURN OFF")
                );
                turnOffBtn.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
                turnOffBtn.click();

                // Click Allow
                UiAutomatorUtils.handleButtonClick(
                        "android:id/button1"
                );

                batteryOptimizationTurnedOff = true;

                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(7));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Return to home page
                forceStop();
                launch();
            } catch (final UiObjectNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    }
}
