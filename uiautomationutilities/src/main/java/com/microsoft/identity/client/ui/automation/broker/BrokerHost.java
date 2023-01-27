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

import com.google.gson.Gson;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import android.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BrokerHost extends AbstractTestBroker {

    private final static String TAG = BrokerHost.class.getSimpleName();
    public final static String BROKER_HOST_APP_PACKAGE_NAME = "com.microsoft.identity.testuserapp";
    public final static String BROKER_HOST_APP_NAME = "Broker Host App";
    public final static String BROKER_HOST_APK = "BrokerHost.apk";
    public final static String OLD_BROKER_HOST_APK = "OldBrokerHost.apk";
    public final static String BROKER_HOST_APK_PROD = "BrokerHostProd.apk";
    public final static String BROKER_HOST_APK_RC = "BrokerHostRC.apk";

    public BrokerHost() {
        super(BROKER_HOST_APP_PACKAGE_NAME, BROKER_HOST_APP_NAME,
                new LocalApkInstaller(), new LocalApkInstaller());
        localApkFileName = BROKER_HOST_APK;
        localUpdateApkFileName = BROKER_HOST_APK;
    }

    public BrokerHost(@NonNull final String brokerHostApkName) {
        super(BROKER_HOST_APP_PACKAGE_NAME, BROKER_HOST_APP_NAME,
                new LocalApkInstaller(), new LocalApkInstaller());
        localApkFileName = brokerHostApkName;
        localUpdateApkFileName = brokerHostApkName;
    }

    public BrokerHost(@NonNull final String brokerHostApkName,
                      @NonNull final String updateBrokerHostApkName) {
        super(BROKER_HOST_APP_PACKAGE_NAME, BROKER_HOST_APP_NAME,
                new LocalApkInstaller(), new LocalApkInstaller());
        localApkFileName = brokerHostApkName;
        localUpdateApkFileName = updateBrokerHostApkName;
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        performDeviceRegistration(username, password, false);
    }

    @Override
    public void performDeviceRegistration(String username, String password, boolean isFederatedUser) {
        Logger.i(TAG, "Performing Device Registration for the given account..");
        performDeviceRegistrationHelper(username);

        // Click the join btn
        final UiObject joinBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndEnabledFlag(
                CommonUtils.getResourceId(
                        getPackageName(), "buttonJoin"
                ), true
        );

        try {
            joinBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        if (isFederatedUser) {
            final AdfsPromptHandler adfsPromptHandler = new AdfsPromptHandler(promptHandlerParameters);
            Logger.i(TAG, "Handle prompt of ADFS login page for Device Registration..");
            // handle ADFS login page
            adfsPromptHandler.handlePrompt(username, password);
        } else {
            final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
            Logger.i(TAG, "Handle prompt of AAD login page for Device Registration..");
            // handle AAD login page
            aadPromptHandler.handlePrompt(username, password);
        }

        try {
            postJoinConfirmHelper(username);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void performSharedDeviceRegistration(String username, String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account..");
        performDeviceRegistrationHelper(username);

        // Click the join shared device btn
        UiObject joinBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndEnabledFlag(
                "com.microsoft.identity.testuserapp:id/buttonJoinSharedDevice", true
        );

        try {
            joinBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }


        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .sessionExpected(false)
                .loginHint(username)
                .build();

        final AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);

        Logger.i(TAG, "Handle prompt of AAD login page for Shared Device Registration..");
        // handle AAD login page
        aadPromptHandler.handlePrompt(username, password);

        try {
            postJoinConfirmHelper(username);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void performSharedDeviceRegistrationDontValidate(@NonNull final String username,
                                                            @NonNull final String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account..");
        performDeviceRegistrationHelper(username);
    }

    private void performDeviceRegistrationHelper(@NonNull final String username) {
        Logger.i(TAG, "Execution of Helper for Device Registration..");
        launch(); // launch Broker Host app

        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }

        // enter upn in text box
        UiAutomatorUtils.handleInput(
                "com.microsoft.identity.testuserapp:id/editTextUsername",
                username
        );

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(4));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void postJoinConfirmHelper(@NonNull final String expectedUpn) throws InterruptedException {
        Logger.i(TAG, "Confirming that Shared Device Registration is successfull or not..");

        // Look for join op completion dialog
        final UiObject joinFinishDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue("Assert join finish dialog", joinFinishDialog.waitForExists(CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG));

        try {
            // Obtain the text from the dialog box
            final String joinFinishDialogText = joinFinishDialog.getText();
            final String joinStatus = joinFinishDialogText.split(":")[1];
            // The status should be successful
            Assert.assertTrue("Assert the status is successful", "SUCCESSFUL".equalsIgnoreCase(joinStatus));

            // dismiss the dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");

            // Adding brief wait to increase reliability of test
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));

            // compare the UPN to make sure joined with the expected account
            final String joinedUpn = getAccountUpn();
            Assert.assertTrue("Assert that the joined account is the expected account", expectedUpn.equalsIgnoreCase(joinedUpn));
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Nullable
    @Override
    public String obtainDeviceId() {
        Logger.i(TAG, "Obtain Device Id..");
        final String resourceButtonId = "com.microsoft.identity.testuserapp:id/buttonDeviceId";
        final String textId = "DeviceId";
        return basicButtonHandler(resourceButtonId, textId);
    }

    @Override
    public void enableBrowserAccess() {
        Logger.i(TAG, "Enable Browser Access..");
        launch();

        if (shouldHandleFirstRun) {
            handleFirstRun();
        }

        // Click enable browser access
        UiAutomatorUtils.handleButtonClick(
                "com.microsoft.identity.testuserapp:id/buttonInstallCert"
        );

        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Install cert
        final UiObject certInstaller = device.findObject(new UiSelector().packageName("com.android.certinstaller"));
        certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        Assert.assertTrue(
                "Microsoft Authenticator - cert installer dialog appears.",
                certInstaller.exists()
        );

        UiAutomatorUtils.handleButtonClick("android:id/button1");
    }

    @Override
    public DeviceAdmin getAdminName() {
        Logger.i(TAG, "Get Admin name..");
        return DeviceAdmin.BROKER_HOST;
    }

    @Override
    public void handleFirstRun() {
        // nothing needed here
    }

    @Override
    public void initialiseAppImpl() {
        // nothing needed here
    }

    @Nullable
    public String getAccountUpn() {
        Logger.i(TAG, "Get Account Upn..");
        final String resourceButtonId = "com.microsoft.identity.testuserapp:id/buttonGetWpjUpn";
        final String textId = "UPN";
        return basicButtonHandler(resourceButtonId, textId);
    }

    @Nullable
    public String getDeviceState() {
        Logger.i(TAG, "Get Device State ..");
        final String resourceButtonId = "com.microsoft.identity.testuserapp:id/buttonDeviceState";
        final String textId = "DeviceState";
        return basicButtonHandler(resourceButtonId, textId);
    }

    @Nullable
    public boolean isDeviceShared() {
        Logger.i(TAG, "Check if device is shared..");
        final String resourceButtonId = "com.microsoft.identity.testuserapp:id/buttonIsDeviceShared";
        final String textId = "DeviceShared";
        final String isDeviceSharedText = basicButtonHandler(resourceButtonId, textId);
        return "Device is shared".equalsIgnoreCase(isDeviceSharedText);
    }

    @Nullable
    public String wpjLeave() {
        Logger.i(TAG, "Wpj Leave ..");
        final String resourceButtonId = "com.microsoft.identity.testuserapp:id/buttonLeave";
        final String textId = "wpjLeave";
        return basicButtonHandler(resourceButtonId, textId);
    }

    @Nullable
    private final String basicButtonHandler(@NonNull final String resourceButtonId,
                                            @NonNull final String textId) {
        launch(); // launch Broker Host app

        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }

        UiAutomatorUtils.handleButtonClick(resourceButtonId);

        // Look for the dialog box
        final UiObject dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );
        Assert.assertTrue("Assert dialog box after button click", dialogBox.exists());
        return getDialogBoxText(dialogBox, textId);
    }

    @Override
    public void overwriteFlights(@Nullable final String flightsJson) {
        Logger.i(TAG, "Overwrite Flights..");
        launch();

        // Sleep for a bit to finish launching brokerHost before scrolling to Overwrite Flights
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // scroll to find the overwrite flights button
        UiAutomatorUtils.obtainChildInScrollable("Overwrite flights (When BrokerHost is the active broker)");
        // input flights string in flights input box
        UiAutomatorUtils.handleInput("com.microsoft.identity.testuserapp:id/editTextFlights", flightsJson);
        // Click Set Flights button
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/overwriteFlightsButton");
    }

    @Override
    public void setFlights(@Nullable final String flightsJson) {
        Logger.i(TAG, "Set Flights..");
        launch();

        // Sleep for a bit to finish launching brokerHost before scrolling to Set Flights
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // scroll to find the set flights button
        UiAutomatorUtils.obtainChildInScrollable("Set Flights");
        // input flights string in flights input box
        UiAutomatorUtils.handleInput("com.microsoft.identity.testuserapp:id/editTextFlights", flightsJson);
        // Click Set Flights button
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/setFlightsButton");
    }


    @Override
    public String getFlights() {
        Logger.i(TAG, "Get Flights..");
        final String text = "Get Flights";
        launch();

        // Sleep for a bit to finish launching brokerHost before scrolling to Get Flights
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            // scroll to find the get flights button and click
            final UiObject getFlightsButton = UiAutomatorUtils.obtainChildInScrollable(text);
            getFlightsButton.click();
            final UiObject flightsObj = UiAutomatorUtils.obtainUiObjectWithResourceId("com.microsoft.identity.testuserapp:id/editTextFlights");
            return flightsObj.getText();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private String getDialogBoxText(@NonNull final UiObject dialogBox,
                                    @NonNull final String textId) {
        try {
            // get the textId if it is there, else return null (in case of error)
            final String[] dialogBoxText = dialogBox.getText().split(":");
            // look for the textId if present
            if (textId.equalsIgnoreCase(dialogBoxText[0])) {
                return dialogBoxText[1];
            } else {
                return null;
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        } finally {
            // dismiss dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        }
    }

    /**
     * Gets all the accounts added
     */
    public List<String> getAllAccounts(final boolean expectingMultipleAccounts) {
        launch();

        final String resourceButtonId = "com.microsoft.identity.testuserapp:id/buttonGetAccounts";
        final String textId = "AccountName";
        if (shouldHandleFirstRun) {
            handleFirstRun(); // handle first run experience
        }
        UiAutomatorUtils.obtainChildInScrollable("Get Accounts");
        UiAutomatorUtils.handleButtonClick(resourceButtonId);
        // Look for the dialog box
        final UiObject dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );
        Assert.assertTrue("Assert get all accounts dialog box exists", dialogBox.exists());

        // As of now we are testing only for 2 accounts.
        List<String> accounts = new ArrayList<>();
        final String accountName = getDialogBoxText(dialogBox, textId);
        if (accountName != null) {
            accounts.add(accountName);
        }
        if (expectingMultipleAccounts) {
            final String accountName2 = getDialogBoxText(dialogBox, textId);
            if (accountName2 != null)
                accounts.add(accountName2);
        }
        return accounts;
    }

    /**
     * Removes the added account
     */
    public void removeAccount(@NonNull final String username) {
        try {
            final UiObject removeAccount = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.identity.testuserapp:id/buttonRemoveAccount"
            );
            final UiObject accountNameTxtBox = UiAutomatorUtils.obtainChildInScrollable("someone@contoso.com");
            accountNameTxtBox.setText(username);
            removeAccount.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Acquire SSO token with provided nonce
     */
    public String acquireSSOToken(@NonNull final String nonce) {
        try {
            // Fill the nonce
            final UiObject nonceTxtBox = UiAutomatorUtils.obtainChildInScrollable("nonce for SSO Token");
            nonceTxtBox.setText(nonce);

            // Click on sso token button
            UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetSsoToken");

            // Get SSOToken
            final UiObject ssoToken = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.identity.testuserapp:id/sso_token"
            );
            final String ssoTokenTxt = ssoToken.getText();
            Assert.assertNotEquals("Assert sso token is not empty", ssoTokenTxt, "");
            return ssoTokenTxt;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Decode SSO token and verify the expected nonce
     */
    public void decodeSSOTokenAndVerifyNonce(@NonNull final String ssoToken,
                                             @NonNull final String nonce) {
        String token = new String(Base64.decode(ssoToken.split("\\.")[1], Base64.NO_WRAP));
        final Map<Object, Object> map = new Gson().fromJson(token, Map.class);
        StringBuilder sb = new StringBuilder();
        final Set<Map.Entry<Object, Object>> set = map.entrySet();
        for (Map.Entry<Object, Object> e : set) {
            sb.append(e.getKey()).append(" => ")
                    .append(e.getValue())
                    .append('\n');
        }
        final String decodedToken = sb.toString();
        if (decodedToken.contains("request_nonce")) {
            final String[] str = decodedToken.split("request_nonce => ");
            if (str.length > 1) {
                Assert.assertEquals(str[1].trim(), nonce);
            } else {
                Assert.fail("decoded token does not contain correct nonce");
            }
        } else {
            Assert.fail("decoded token does not contain correct nonce");
        }
    }

    /**
     * Confirm that the calling app is not verified
     */
    public void confirmCallingAppNotVerified() {
        // Look for the dialog box
        final UiObject dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );
        Assert.assertTrue("Assert not verified dialog box", dialogBox.exists());
        try {
            if (!dialogBox.getText().contains("Calling app could not be verified")) {
                Assert.fail("Could not find the string 'calling app could not be verified' in the msg displayed in the dialog");
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        } finally {
            // dismiss dialog
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        }
    }

    /**
     * Check if the Device Code Flow option shows up in sign in flow.
     * @param tenantId tenant ID to use in Join Tenant
     * @throws UiObjectNotFoundException
     */
    public void checkForDcfOption(@Nullable final String tenantId){
        final String tenantIdToUse;

        // If no tenant ID is specified, default to microsoft tenant
        if (tenantId == null) {
            tenantIdToUse = "72f988bf-86f1-41af-91ab-2d7cd011db47";
        } else {
            tenantIdToUse = tenantId;
        }

        final String joinTenantButtonId = "com.microsoft.identity.testuserapp:id/buttonJoinTenant";
        final String joinTenantEditTestId = "com.microsoft.identity.testuserapp:id/editTextTenantId";

        launch();

        UiAutomatorUtils.handleInput(joinTenantEditTestId, tenantIdToUse);

        UiAutomatorUtils.handleButtonClick(joinTenantButtonId);

        // Apparently, there are two UI objects with exact text "Sign-in options", one is a button the other is a view
        // Have to specify the search to button class
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiObject optionsObject = device.findObject(new UiSelector()
                .text("Sign-in options").className("android.widget.Button"));

        try {
            optionsObject.click();
        } catch (UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
        UiAutomatorUtils.handleButtonClickForObjectWithText("Sign in from another device");

        // Doesn't look like the page with the device code is readable to the UI automation,
        // this is a sufficient stopping point
    }
}
