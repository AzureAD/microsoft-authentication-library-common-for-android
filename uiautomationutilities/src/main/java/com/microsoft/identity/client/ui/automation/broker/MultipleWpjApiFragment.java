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

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.interaction.IPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ThreadUtils;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleWpjApiFragment extends AbstractBrokerHost {
    private static final String DEVICE_REGISTRATION_BUTTON_ID = "button_device_registration";
    private static final String SHARED_DEVICE_REGISTRATION_BUTTON_ID = "button_device_registration_shared";
    private static final String GET_ALL_RECORDS_BUTTON_ID = "button_mwpj_get_records";
    private static final String INSTALL_CERTIFICATE_BUTTON_ID = "button_mwpj_install_cert";
    private static final String GET_RECORD_BY_TENANT_BUTTON_ID = "button_mwpj_get_record_by_tenant";
    private static final String GET_RECORD_BY_UPN_BUTTON_ID = "button_mwpj_get_record_by_upn";
    private static final String UNREGISTER_BUTTON_ID = "button_mwpj_leave";
    private static final String GET_DEVICE_STATE_BUTTON_ID = "button_mwpj_get_state";
    private static final String GET_DEVICE_TOKEN_BUTTON_ID = "button_mwpj_get_device_token";
    private static final String GET_BLOB_BUTTON_ID = "button_mwpj_get_blob";

    private final BrokerHost brokerHost;

    public MultipleWpjApiFragment(@NonNull final BrokerHost brokerHost) {
        super();
        this.brokerHost = brokerHost;
    }


    /**
     * This method launches the broker host app to a specified fragment.
     */
    @Override
    public void launch() {
        launch(BrokerHostNavigationMenuItem.MULTIPLE_WPJ_API);
    }

    /**
     * Install the certificate on the device.
     */
    public void installCertificate(@NonNull final String identifier) {
        launch();
        selectDeviceRegistrationRecord(identifier);
        clickButton(INSTALL_CERTIFICATE_BUTTON_ID);
        ThreadUtils.sleepSafely(3000, "Sleep failed", "Interrupted");
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        final UiObject certInstaller = device.findObject(new UiSelector().packageName(CERT_INSTALLER_PACKAGE_NAME));
        certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        Assert.assertTrue(
                "Microsoft Authenticator - cert installer dialog appears.",
                certInstaller.exists()
        );
        UiAutomatorUtils.handleButtonClick(DIALOG_BOX_OK_BUTTON_RESOURCE_ID);
        ThreadUtils.sleepSafely(1000, "Sleep failed", "Interrupted");
        UiAutomatorUtils.handleButtonClick(DIALOG_BOX_OK_BUTTON_RESOURCE_ID);
        dismissDialogBoxAndAssertContainsText("true");
    }

    public List<Map<String, String>> getAllRecords() {
        launch();
        final List<Map<String, String>> records = new ArrayList<>();
        clickButton(GET_ALL_RECORDS_BUTTON_ID);
        final String dialogBoxText = dismissDialogBoxAndGetText();
        final String dialogBoxTextNoBrackets = dialogBoxText
                .replace("[", "")
                .replace("]", "");
        if (StringUtil.isNullOrEmpty(dialogBoxTextNoBrackets)) {
            return records;
        }
        final String[] deviceRegistrationRecordsRaw = dialogBoxTextNoBrackets.split(",");
        for (final String record : deviceRegistrationRecordsRaw) {
            records.add(recordStringToMap(record));
        }
        return records;
    }

    private Map<String, String> recordStringToMap(@NonNull final String recordRaw) {
        final String[] recordProperties = recordRaw.split(System.lineSeparator());
        Assert.assertTrue(
                "Device registration record should have 4 or 5 lines",
                recordProperties.length == 4 || recordProperties.length == 5
        );
        Assert.assertTrue(
                "Record should have a valid tenant id." + recordProperties[0],
                recordProperties[0].trim().startsWith("TenantId:")
        );
        Assert.assertTrue(
                "Record should have a valid upn." + recordProperties[1],
                recordProperties[1].trim().startsWith("Upn:")
        );
        Assert.assertTrue(
                "Record should have a valid device id." + recordProperties[2],
                recordProperties[2].trim().startsWith("DeviceId:")
        );
        Assert.assertTrue(
                "Record should have a shared status tag." + recordProperties[3],
                recordProperties[3].trim().startsWith("isShared:")
        );
        // The 5th line is the Account name of the device registration record, which is optional.
        final Map<String, String> deviceRegistrationRecord = new HashMap<>();
        deviceRegistrationRecord.put("TenantId", recordProperties[0].replace("TenantId:", "").trim());
        deviceRegistrationRecord.put("Upn", recordProperties[1].replace("Upn:", "").trim());
        deviceRegistrationRecord.put("DeviceId", recordProperties[2].replace("DeviceId:", "").trim());
        deviceRegistrationRecord.put("isShared", recordProperties[3].replace("isShared:", "").trim());
        return deviceRegistrationRecord;
    }


    public Map<String, String> getRecordByTenantId(@NonNull final String tenantId) {
        launch();
        fillTextBox(TENANT_EDIT_TEXT, tenantId);
        clickButton(GET_RECORD_BY_TENANT_BUTTON_ID);
        return recordStringToMap(dismissDialogBoxAndGetText());
    }

    public Map<String, String> getRecordByUpn(@NonNull final String upn) {
        launch();
        fillTextBox(USERNAME_EDIT_TEXT, upn);
        clickButton(GET_RECORD_BY_UPN_BUTTON_ID);
        return recordStringToMap(dismissDialogBoxAndGetText());
    }

    /**
     * This method clicks on the device registration record with the given text
     *
     * @param text the text of the button to be clicked
     */
    static private void selectDeviceRegistrationRecord(@NonNull final String text) {
        final UiObject deviceRegistrationRecordItem
                = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(text, TextView.class);
        try {
            deviceRegistrationRecordItem.click();
        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could not click on the object with resource text: " + text, e);
        }
    }

    public void unregister(@NonNull final String identifier) {
        launch();
        selectDeviceRegistrationRecord(identifier);
        clickButton(UNREGISTER_BUTTON_ID);
        dismissDialogBoxAndAssertContainsText("Removed");
    }

    public String getDeviceState(@NonNull final String identifier) {
        launch();
        selectDeviceRegistrationRecord(identifier);
        clickButton(GET_DEVICE_STATE_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }

    public String getDeviceToken(@NonNull final String identifier) {
        launch();
        selectDeviceRegistrationRecord(identifier);
        clickButton(GET_DEVICE_TOKEN_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }

    public String getBlob(@NonNull final String tenantId) {
        launch();
        fillTextBox(TENANT_EDIT_TEXT, tenantId);
        clickButton(GET_BLOB_BUTTON_ID);
        return dismissDialogBoxAndGetText();
    }

    /**
     * Performs a device registration
     *
     * @param username the username to be used for device registration
     * @param password the password to be used for device registration
     */
    public String performDeviceRegistrationDontValidate(String username, String password) {
        return performDeviceRegistrationInternal(
                username,
                password,
                false,
                false
        );
    }

    /**
     * Performs a device registration
     *
     * @param username the username to be used for device registration
     * @param password the password to be used for device registration
     */
    public void performDeviceRegistration(String username, String password) {
        performDeviceRegistrationInternal(
                username,
                password,
                false,
                true
        );
    }

    /**
     * Performs a device registration
     *
     * @param username the username to be used for device registration
     * @param password the password to be used for device registration
     */
    public void performSharedDeviceRegistration(String username, String password) {
        performDeviceRegistrationInternal(
                username,
                password,
                true,
                true
        );
    }


    /**
     * Performs a device registration
     *
     * @param username                      the username to be used for device registration
     * @param password                      the password to be used for device registration
     * @param isSharedDevice                true if the device is a shared device
     */
    private String performDeviceRegistrationInternal(@NonNull final String username,
                                                     @NonNull final String password,
                                                     final boolean isSharedDevice,
                                                     final boolean shouldValidate) {
        launch();
        fillTextBox(USERNAME_EDIT_TEXT, username);

        if (isSharedDevice) {
            clickButton(SHARED_DEVICE_REGISTRATION_BUTTON_ID);
        } else {
            clickButton(DEVICE_REGISTRATION_BUTTON_ID);
        }

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(brokerHost)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();


        final IPromptHandler promptHandler = getPromptHandler(false, promptHandlerParameters);
        promptHandler.handlePrompt(username, password);
        ThreadUtils.sleepSafely(3000,"performDeviceRegistrationInternal", "Waiting for result to be returned from the broker");
        if (shouldValidate) {
            dismissDialogBoxAndAssertContainsText("SUCCESS");
        } else {
            return dismissDialogBoxAndGetText();
        }
        return null;
    }

}
