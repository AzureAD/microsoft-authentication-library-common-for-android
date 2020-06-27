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
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import lombok.Getter;

@Getter
public class BrokerCompanyPortal extends AbstractTestBroker implements ITestBroker, IMdmAgent {

    public final static String COMPANY_PORTAL_APP_PACKAGE_NAME = "com.microsoft.windowsintune.companyportal";
    public final static String COMPANY_PORTAL_APP_NAME = "Intune Company Portal";
    public final static String COMPANY_PORTAL_APK = "CompanyPortal.apk";

    public BrokerCompanyPortal() {
        super(COMPANY_PORTAL_APP_PACKAGE_NAME, COMPANY_PORTAL_APP_NAME, new PlayStore());
        localApkFileName = COMPANY_PORTAL_APK;
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        enrollDevice(username, password); // enrolling device also performs device registration
    }

    @Override
    public void performSharedDeviceRegistration(@NonNull final String username,
                                                @NonNull final String password) {
        //TODO implement shared device registration for CP
        throw new UnsupportedOperationException("Unimplemented!");
    }

    @Override
    public void handleFirstRun() {
        return; // nothing need here
    }

    @Override
    public void enrollDevice(String username, String password) {
        launch(); // launch CP app

        handleFirstRun(); // handle CP first run

        // click Sign In button on CP welcome page
        UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/sign_in_button");

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .consentPageExpected(false)
                .expectingNonZeroAccountsInCookie(false)
                .sessionExpected(false)
                .loginHintProvided(false)
                .build();

        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

        // handle AAD login page
        aadPromptHandler.handlePrompt(username, password);

        // click the activate device admin btn
        try {
            final UiObject accessSetupScreen = UiAutomatorUtils.obtainUiObjectWithText("Access Setup");
            Assert.assertTrue(accessSetupScreen.exists());

            // click on BEGIN button to start enroll
            UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/setup_positive_button");

            // click CONTINUE to ack privacy page
            UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/ContinueButton");

            // click NEXT to ack Android system permissions requirements
            UiAutomatorUtils.handleButtonClick("com.microsoft.windowsintune.companyportal:id/bullet_list_page_forward_button");

            // grant permission
            CommonUtils.grantPackagePermission();

            // Confirm on page to activate CP as device admin
            final UiObject activeDeviceAdminPage = UiAutomatorUtils.obtainUiObjectWithText("Activate device admin");
            Assert.assertTrue(activeDeviceAdminPage.exists());

            // scroll down the recycler view to find activate device admin btn
            final UiObject activeDeviceAdminBtn = UiAutomatorUtils.obtainChildInScrollable(
                    "Activate this device admin app"
            );

            assert activeDeviceAdminBtn != null;

            // click on activate device admin btn
            activeDeviceAdminBtn.click();

            // make sure we are on the page to complete setup
            final UiObject setupCompletePage = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.windowsintune.companyportal:id/setup_title"
            );

            Assert.assertTrue(setupCompletePage.exists());

            // click on DONE to complete setup
            UiAutomatorUtils.handleButtonClick(
                    "com.microsoft.windowsintune.companyportal:id/setup_center_button"
            );
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}
