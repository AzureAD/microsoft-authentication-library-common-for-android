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

import com.microsoft.identity.client.ui.automation.app.IApp;

/**
 * An interface for an Android broker being used during a UI Test. We can perform operations such as
 * device registration with this broker during the test.
 */
public interface ITestBroker extends IApp {

    /**
     * Handle the broker account picker. Clicks on the list item associated to the supplied upn
     *
     * @param username upn for the account to select in account picker
     */
    void handleAccountPicker(String username);

    /**
     * Perform device registration with supplied username
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performDeviceRegistration(String username, String password);

    /**
     * Perform shared device registration with supplied username. This user must be a cloud device
     * admin for the registration to actually succeed.
     *
     * @param username username of the account to use for registration
     * @param password password of the account to use for registration
     */
    void performSharedDeviceRegistration(String username, String password);
}
