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

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.interaction.IPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public class SingleWpjApiFragment extends AbstractBrokerHost {

    private final static String CERT_INSTALLER_PACKAGE_NAME = "com.android.certinstaller";
    // Resource Id for the buttons
    public final static String GET_WPJ_ACCOUNT_BUTTON_ID = "button_get_wpj_upn";
    private final static String LEAVE_BUTTON_ID = "button_leave";
    public final static String GET_DEVICE_STATE_BUTTON_ID = "button_get_device_state";
    public final static String IS_DEVICE_SHARED_BUTTON_ID = "button_is_device_shared";
    public final static String GET_DEVICE_ID_BUTTON_ID = "button_get_device_id";
    public final static String JOIN_BUTTON_ID = "button_join";
    public final static String JOIN_SHARED_DEVICE_BUTTON_ID = "button_join_shared_device";
    public final static String INSTALL_CERT_BUTTON_ID = "button_install_cert";
    public final static String JOIN_TENANT_BUTTON_ID = "button_join_tenant";
    // Resource Id for the edit text
    public final static String TENANT_EDIT_TEXT ="edit_text_tenant_id";


    public void wpjLeave() {
        clickButton(LEAVE_BUTTON_ID);
        dismissDialogBoxAndGetText();
    }

    public String getWpjAccount() {
        clickButton(GET_WPJ_ACCOUNT_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }
    public String getDeviceState() {
        clickButton(GET_DEVICE_STATE_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }

    public String isDeviceShared() {
        clickButton(IS_DEVICE_SHARED_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }

    public String getDeviceId() {
        clickButton(GET_DEVICE_ID_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }


    public void clickJoinTenant(String tenantId) {
        fillTextBox(TENANT_EDIT_TEXT, tenantId);
        clickButton(JOIN_TENANT_BUTTON_ID);
    }
    public void installCertificate() {
        clickButton(INSTALL_CERT_BUTTON_ID);
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        final UiObject certInstaller = device.findObject(new UiSelector().packageName(CERT_INSTALLER_PACKAGE_NAME));
        certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        Assert.assertTrue(
                "Microsoft Authenticator - cert installer dialog appears.",
                certInstaller.exists()
        );
        UiAutomatorUtils.handleButtonClick(DIALOG_BOX_OK_BUTTON_RESOURCE_ID);
    }

    public void performSharedDeviceRegistration(String username, String password, @NonNull final ITestBroker testBroker) {
        fillTextBox(USERNAME_EDIT_TEXT, username);
        clickButton(JOIN_SHARED_DEVICE_BUTTON_ID);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(testBroker)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);
        dismissDialogBoxAndAssertContainsText("SUCCESSFUL");
        delay(2);
    }


    public void performDeviceRegistration(String username, String password, boolean isFederatedUser, @NonNull final ITestBroker testBroker) {
        fillTextBox(USERNAME_EDIT_TEXT, username);
        clickButton(JOIN_BUTTON_ID);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(testBroker)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        final IPromptHandler promptHandler = getPromptHandler(isFederatedUser, promptHandlerParameters);
        promptHandler.handlePrompt(username, password);
        dismissDialogBoxAndAssertContainsText("SUCCESSFUL");
        delay(2);
    }

    private MicrosoftStsPromptHandler getPromptHandler(final boolean isFederatedUser, @NonNull final PromptHandlerParameters promptHandlerParameters) {
        if (isFederatedUser) {
            // handle ADFS login page
            return new AdfsPromptHandler(promptHandlerParameters);
        } else {
            // handle AAD login page
            return  new AadPromptHandler(promptHandlerParameters);
        }
    }

    @Override
    public void launch() {
        launch(BrokerHostNavigationMenuItem.SINGLE_WPJ_API);
    }
}
