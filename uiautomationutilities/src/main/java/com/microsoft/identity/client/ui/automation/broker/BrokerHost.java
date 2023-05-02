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
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import android.util.Base64;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrokerHost extends AbstractTestBroker {
    private final static String TAG = BrokerHost.class.getSimpleName();
    // tenant id where lab api and key vault api is registered
    private final static String LAB_API_TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";
    // name for broker host APKs
    public final static String BROKER_HOST_APK = "BrokerHost.apk";
    public final static String OLD_BROKER_HOST_APK = "OldBrokerHost.apk";
    public final static String BROKER_HOST_APK_PROD = "BrokerHostProd.apk";
    public final static String BROKER_HOST_APK_RC = "BrokerHostRC.apk";
    // Fragments for BrokerHost
    public final BrokerFlightsFragment brokerFlightsFragment;
    public final BrokerApiFragment brokerApiFragment;
    public final SingleWpjApiFragment singleWpjApiFragment;
    public final MultipleWpjApiFragment multipleWpjApiFragment;

    public BrokerHost() {
        this(BROKER_HOST_APK, BROKER_HOST_APK);
    }

    public BrokerHost(@NonNull final String brokerHostApkName) {
        this(brokerHostApkName, brokerHostApkName);
    }

    public BrokerHost(@NonNull final String brokerHostApkName,
                      @NonNull final String updateBrokerHostApkName) {
        super(
                AbstractBrokerHost.BROKER_HOST_APP_PACKAGE_NAME,
                AbstractBrokerHost.BROKER_HOST_APP_NAME,
                new LocalApkInstaller(),
                new LocalApkInstaller()
        );
        localApkFileName = brokerHostApkName;
        localUpdateApkFileName = updateBrokerHostApkName;
        brokerFlightsFragment = new BrokerFlightsFragment();
        brokerApiFragment = new BrokerApiFragment();
        singleWpjApiFragment = new SingleWpjApiFragment();
        multipleWpjApiFragment = new MultipleWpjApiFragment();
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(username, password, false, this, null);
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password,
                                          @NonNull final PromptHandlerParameters promptHandlerParameters) {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(username, password, false, this, promptHandlerParameters);
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    @Override
    public void performDeviceRegistration(String username, String password, boolean isFederatedUser) {
        Logger.i(TAG, "Performing Device Registration for the given account..");
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(username, password, isFederatedUser, this, null);
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    @Override
    public void performSharedDeviceRegistration(String username, String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account..");
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performSharedDeviceRegistration(username, password, this);
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    @Override
    public void performSharedDeviceRegistrationDontValidate(@NonNull final String username,
                                                            @NonNull final String password) {
        throw new UnsupportedOperationException("This method is not supported in BrokerHost");
    }

    @Nullable
    @Override
    public String obtainDeviceId() {
        singleWpjApiFragment.launch();
        return singleWpjApiFragment.getDeviceId();
    }

    @Override
    public void enableBrowserAccess() {
        Logger.i(TAG, "Enable Browser Access..");
        singleWpjApiFragment.launch();
        singleWpjApiFragment.installCertificate();
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
        singleWpjApiFragment.launch();
        return singleWpjApiFragment.getWpjAccount();
    }

    @Nullable
    public String getDeviceState() {
        singleWpjApiFragment.launch();
        return singleWpjApiFragment.getDeviceState();
    }

    public void wpjLeave() {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.wpjLeave();
    }

    @Override
    public void overwriteFlights(@NonNull final String flightsJson) {
        Logger.i(TAG, "Overwrite Flights..");
        brokerFlightsFragment.launch();
        brokerFlightsFragment.overWriteLocalFlights(flightsJson);
    }

    @Override
    public void setFlights(@NonNull final String key, @NonNull final String value) {
        Logger.i(TAG, "Set Flights..");
        brokerFlightsFragment.launch();
        brokerFlightsFragment.setLocalFlight(key, value);
    }

    @Override
    public String getFlights() {
        Logger.i(TAG, "Get Flights..");
        brokerFlightsFragment.launch();
        return brokerFlightsFragment.getFlights();
    }

    /**
     * Gets all the accounts added
     */
    public List<String> getAllAccounts() {
        brokerApiFragment.launch();
        return brokerApiFragment.getAccounts();
    }

    /**
     * Removes the added account
     */
    public void removeAccount(@NonNull final String username) {
        brokerApiFragment.launch();
        brokerApiFragment.removeAccounts(username);
    }

    /**
     * Acquire SSO token with provided nonce
     */
    public String acquireSSOToken(@NonNull final String nonce) {
        brokerApiFragment.launch();
        final String ssoToken = brokerApiFragment.acquireSsoToken(nonce);
        Assert.assertNotEquals("Assert sso token is not empty", ssoToken, "");
        return ssoToken;
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
        AbstractBrokerHost.dismissDialogBoxAndAssertContainsText("Calling App is not verified");
    }

    /**
     * Check if the Device Code Flow option shows up in sign in flow.
     *
     * @param tenantId tenant ID to use in Join Tenant
     */
    public void checkForDcfOption(@Nullable final String tenantId) {
        final String tenantIdToUse;

        // If no tenant ID is specified, default to microsoft tenant
        if (tenantId == null) {
            tenantIdToUse = LAB_API_TENANT_ID;
        } else {
            tenantIdToUse = tenantId;
        }

        singleWpjApiFragment.launch();
        singleWpjApiFragment.clickJoinTenant(tenantIdToUse);

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

    public void performDeviceRegistrationMultiple(String username, String password) {
        Logger.i(TAG, "Performing Device Registration for the given account..");
        multipleWpjApiFragment.launch();
        multipleWpjApiFragment.performDeviceRegistration(username, password, this);
    }

    public void installCertificateMultiple(@NonNull final String tenantId) {
        Logger.i(TAG, "Installing Certificate..");
        multipleWpjApiFragment.launch();
        multipleWpjApiFragment.installCertificate(tenantId);
    }

    public List<Map<String, String>> getAllRecords() {
        Logger.i(TAG, "Get All Records..");
        multipleWpjApiFragment.launch();
        return multipleWpjApiFragment.getAllRecords();
    }

    public Map<String, String> getRecordByTenantId(@NonNull final String tenantId) {
        Logger.i(TAG, "Get records by tenant id..");
        multipleWpjApiFragment.launch();
        return multipleWpjApiFragment.getRecordByTenantId(tenantId);
    }

    public Map<String, String> getRecordByUpn(@NonNull final String upn) {
        Logger.i(TAG, "Get records by upn..");
        multipleWpjApiFragment.launch();
        return multipleWpjApiFragment.getRecordByUpn(upn);
    }

    public void unregisterDeviceMultiple(@NonNull final String identifier) {
        Logger.i(TAG, "Unregister Device..");
        multipleWpjApiFragment.launch();
        multipleWpjApiFragment.unregister(identifier);
    }

    public void enableMultipleWpj() {
        Logger.i(TAG, "Enable Multiple Account..");
        brokerFlightsFragment.launch();
        brokerFlightsFragment.setLocalFlight("EnableMultipleWorkplaceJoinGA", "true");
    }

    public void disableMultipleWpj() {
        Logger.i(TAG, "Enable Multiple Account..");
        brokerFlightsFragment.launch();
        brokerFlightsFragment.setLocalFlight("EnableMultipleWorkplaceJoinGA", "false");
    }
}