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

import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;

/**
 * An interface for an Android broker being used during a UI Test. We can perform operations such as
 * device registration with this broker during the test.
 */
public interface ITestBroker extends IApp {

    /**
     * Handle the broker account picker. Clicks on the list item associated to the supplied upn, or
     * if the upn is not provided, then clicks on "Use another account" button.
     *
     * @param username upn for the account to select in account picker
     */
    void handleAccountPicker(String username);

    /**
     * Perform device registration with supplied username.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performDeviceRegistration(String username, String password);

    /**
     * Perform device registration with supplied username.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     * @param isFederatedUser set to true if the user is a federated user
     */
    void performDeviceRegistration(String username, String password, boolean isFederatedUser);

    /**
     * Perform shared device registration with supplied username. This user must be a cloud device
     * admin for the registration to actually succeed.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performSharedDeviceRegistration(String username, String password);

    /**
     * Perform shared device registration with supplied username. This user must be a cloud device
     * admin for the registration to actually succeed. This method excludes checking if is in shared device mode.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performSharedDeviceRegistrationDontValidate(String username, String password);

    /**
     * Perform device registration from the Join Activity using the supplied user account.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performJoinViaJoinActivity(String username, String password);

    /**
     * Perform device registration from the Join Activity using the supplied user account.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     * @param isFederatedUser true if it is a federated user
     */
    void performJoinViaJoinActivity(String username, String password, boolean isFederatedUser);

    /**
     * Confirm that device registered with the supplied UPN by comparing it with the UPN
     * displayed in Join Activity.
     *
     * @param username the username of the account for which to confirm registration
     */
    void confirmJoinInJoinActivity(String username);

    /**
     * Obtain the device id of the registered device from the broker.
     *
     * @return a String representing the device id of the registered device
     */
    @Nullable
    String obtainDeviceId();

    /**
     * Enable browser access from this broker.
     */
    void enableBrowserAccess();

    /**
     * The admin name for this broker app. This name is used to represent the broker app as an
     * admin on the Device Administrator page on the Android settings app when enabled as a
     * device admin.
     *
     * @return the {@link DeviceAdmin} name for this broker app
     */
    DeviceAdmin getAdminName();

    /**
     * Overwrite the whole flight information.
     * @param flightsJson the json representation of the flight key and value pairs {"key1":"value"}.
     */
    void overwriteFlights(@Nullable final String flightsJson);

    /**
     * Set flight informations.
     * @param flightsJson the json representation of the flight key and value pairs {"key1":"value"}.
     */
    void setFlights(@Nullable final String flightsJson);

    /**
     * The flight information set for this broker app.
     *
     * @return the flight information set for this broker app
     */
    String getFlights();
}
