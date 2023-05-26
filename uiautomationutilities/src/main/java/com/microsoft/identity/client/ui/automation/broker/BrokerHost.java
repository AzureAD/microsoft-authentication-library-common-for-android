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

import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.IPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;

import org.junit.Assert;

import java.util.List;

import lombok.Getter;

/**
 * A model for interacting with the BrokerHost app during UI Test.
 * <p>
 * By default all the {@link ITestBroker} operations are performed using the {@link SingleWpjApiFragment} class.
 * if you want to perform specif broker host operations, you need to call the corresponding fragment class
 * and then call the corresponding method.
 * <p>
 * Legacy WPJ operations are contained in the {@link SingleWpjApiFragment} class.
 * Multiple WPJ operations are contained in the {@link MultipleWpjApiFragment} class.
 * Broker API operations are contained in the {@link BrokerApiFragment} class.
 * Broker Flights operations are contained in the {@link BrokerFlightsFragment} class.
 *
 */
public class BrokerHost extends AbstractTestBroker {
    private final static String TAG = BrokerHost.class.getSimpleName();

    // flight to enable/disable the multiple wpj feature
    private final static String FLIGHT_FOR_WORKPLACE_JOIN_CONTROLLER = "ENABLE_MULTIPLE_WORKPLACE_JOIN_PP";
    // name for broker host APKs
    public final static String BROKER_HOST_APK = "BrokerHost.apk";
    public final static String OLD_BROKER_HOST_APK = "OldBrokerHost.apk";
    public final static String BROKER_HOST_APK_PROD = "BrokerHostProd.apk";
    public final static String BROKER_HOST_APK_RC = "BrokerHostRC.apk";
    // Fragments for BrokerHost
    public final BrokerFlightsFragment brokerFlightsFragment;
    public final BrokerApiFragment brokerApiFragment;
    @Getter
    public final SingleWpjApiFragment singleWpjApiFragment;
    // If you're writing test specifically for MWPJ, use this fragment
    // the default behavior for brokerHost app is to use the SingleWpjApiFragment
    @Getter
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
        multipleWpjApiFragment = new MultipleWpjApiFragment(this);
    }

    @Override
    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password) {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(
                username,
                password,
                false,
                false,
                getDefaultBrokerPromptHandlerParameters(username)
        );
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    @Override
    public void performDeviceRegistration(@NonNull String username,
                                          @NonNull String password,
                                          boolean isFederatedUser) {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(
                username,
                password,
                isFederatedUser,
                false,
                getDefaultBrokerPromptHandlerParameters(username)
        );
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    @Override
    public void performSharedDeviceRegistration(String username, String password) {
        Logger.i(TAG, "Performing Shared Device Registration for the given account..");
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(
                username,
                password,
                false,
                true,
                getDefaultBrokerPromptHandlerParameters(username)
        );
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

    public void performDeviceRegistration(@NonNull final String username,
                                          @NonNull final String password,
                                          @NonNull final PromptHandlerParameters promptHandlerParameters) {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.performDeviceRegistration(
                username,
                password,
                false,
                false,
                promptHandlerParameters
        );
        final String joinedUpn = singleWpjApiFragment.getWpjAccount();
        Assert.assertTrue("Assert that the joined account is the expected account", username.equalsIgnoreCase(joinedUpn));
    }

    public void performDeviceRegistrationLegacyApp(@NonNull final String username,
                                          @NonNull final String password) {
        final String inputResourceId = CommonUtils.getResourceId(
                AbstractBrokerHost.BROKER_HOST_APP_PACKAGE_NAME,
                "editTextUsername"
        );
        UiAutomatorUtils.handleInput(inputResourceId, username);

        final String buttonResourceId = CommonUtils.getResourceId(
                AbstractBrokerHost.BROKER_HOST_APP_PACKAGE_NAME,
                "buttonJoin"
        );
        UiAutomatorUtils.handleButtonClick(buttonResourceId);

        final IPromptHandler promptHandler = new AadPromptHandler(getDefaultBrokerPromptHandlerParameters(username));
        promptHandler.handlePrompt(username, password);

        final String dialogMessage = AbstractBrokerHost.dismissDialogBoxAndGetText();
        Assert.assertTrue("Assert that the joined account is the expected account", dialogMessage.contains("SUCCESS"));
    }

    public String obtainDeviceIdLegacyApp() {
        final String buttonGetDeviceId = CommonUtils.getResourceId(
                AbstractBrokerHost.BROKER_HOST_APP_PACKAGE_NAME,
                "buttonDeviceId"
        );
        UiAutomatorUtils.handleButtonClick(buttonGetDeviceId);
        final String dialogMessage = AbstractBrokerHost.dismissDialogBoxAndGetText();
        return dialogMessage.replace("DeviceId:", "");
    }


    public void wpjLeave() {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.wpjLeave();
    }

    public void clickJoinTenant(@NonNull final String tenantId) {
        singleWpjApiFragment.launch();
        singleWpjApiFragment.clickJoinTenant(tenantId);
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

    public String dismissDialog() {
        return AbstractBrokerHost.dismissDialogBoxAndGetText();
    }

    public void enableMultipleWpj() {
        Logger.i(TAG, "Enable Multiple WPJ..");
        brokerFlightsFragment.launch();
        brokerFlightsFragment.selectLocalProvider();
        brokerFlightsFragment.setLocalFlight(FLIGHT_FOR_WORKPLACE_JOIN_CONTROLLER, "true");
        ThreadUtils.sleepSafely(500, TAG, "Wait before force stop.");
        forceStop();
        ThreadUtils.sleepSafely(500, "TAG", "Wait before launch.");
        launch();
    }

    public void disableMultipleWpj() {
        Logger.i(TAG, "Disable Multiple WPJ..");
        brokerFlightsFragment.launch();
        brokerFlightsFragment.selectLocalProvider();
        brokerFlightsFragment.setLocalFlight(FLIGHT_FOR_WORKPLACE_JOIN_CONTROLLER, "false");
        ThreadUtils.sleepSafely(500, TAG, "Wait before force stop.");
        forceStop();
        ThreadUtils.sleepSafely(500, TAG, "Wait before launch.");
        launch();
    }
}
