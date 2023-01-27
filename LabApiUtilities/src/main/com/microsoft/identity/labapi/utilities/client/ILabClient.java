// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.labapi.utilities.client;

import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import java.util.List;

import lombok.NonNull;

/**
 * An interface describing the operations that need to be performed by a Lab Api Client.
 */
public interface ILabClient {

    /**
     * Load an existing account from Lab Api based on the provided query.
     *
     * @param labQuery parameters that determine what kind of account to fetch from lab
     * @return a {@link LabAccount} object
     * @throws LabApiException if an error occurs while trying to fetch account from lab
     */
    ILabAccount getLabAccount(LabQuery labQuery) throws LabApiException;

    /**
     * Load an existing account from Lab Api based on the upn given.
     *
     * @param upn upn of the desired lab account
     * @return a {@link LabAccount} object
     * @throws LabApiException if an error occurs while trying to fetch account from lab
     */
    ILabAccount getLabAccount(@NonNull final String upn) throws LabApiException;

    /**
     * Loads existing account(s) from Lab Api based on the provided query.
     *
     * @param labQuery parameters that determine what kind of account(s) to fetch from lab
     * @return a list of {@link LabAccount} objects
     * @throws LabApiException if an error occurs while trying to fetch account(s) from lab
     */
    List<ILabAccount> getLabAccounts(LabQuery labQuery) throws LabApiException;

    /**
     * Create and return a new temp AAD user using Lab Api.
     *
     * @param tempUserType the {@link TempUserType} of the user to create
     * @return a {@link LabAccount} object
     * @throws LabApiException if an error occurs while trying to fetch account from lab
     */
    ILabAccount createTempAccount(TempUserType tempUserType) throws LabApiException;

    /**
     * Loads guest account from Lab Api based on the provided query.
     *
     * @param labQuery parameters that determine what kind of guest account to fetch
     * @return a {@link LabGuestAccount} object
     * @throws LabApiException if an error occurs while trying to fetch guest account from lab
     */
    LabGuestAccount loadGuestAccountFromLab(final LabQuery labQuery) throws LabApiException;

    /**
     * Get the password for a guest account.
     *
     * @param guestUser the guest account to be fetched from
     * @return a String containing the password for the guest account
     * @throws LabApiException if an error occurs while trying to fetch the password
     */
    String getPasswordForGuestUser(final LabGuestAccount guestUser) throws LabApiException;

    /**
     * Get the value of a secret from Lab Api. This primarily includes secrets like passwords for
     * accounts but may also be used for any other secret that the Lab has stored in their KeyVault.
     *
     * @param secretName the name (identifier) of the secret that should be loaded
     * @return a String containing the value of the secret
     * @throws LabApiException if an error occurs while trying to load secret from lab
     */
    String getSecret(String secretName) throws LabApiException;

    /**
     * Reset the password for the username given, then reset it back to the original password.
     *
     * @param upn username of the user that will have their password reset
     * @return boolean showing if the reset was successful
     * @throws LabApiException if an error occurs while password is being reset
     */
    boolean resetPassword(@NonNull final String upn) throws LabApiException;

    /**
     * Reset the password for the username given, then reset it back to the original password.
     * This method allows for repeated reset attempts if previous attempts fail.
     *
     * @param upn           username of the user that will have their password reset
     * @param resetAttempts number of attempts to reset the password
     * @return boolean showing if the reset was successful
     * @throws LabApiException if an error occurs while password is being reset
     */
    boolean resetPassword(@NonNull final String upn,
                          final int resetAttempts) throws LabApiException;

    /**
     * Delete the specified device from AAD using the Lab Api.
     *
     * @param upn      the upn of the owner of this device
     * @param deviceId the device id of the device to be deleted
     * @return a boolean indicated if device has been deleted
     * @throws LabApiException if an error occurs while trying to delete the device
     */
    boolean deleteDevice(String upn, String deviceId) throws LabApiException;

    /**
     * Attempts deleting the specified device from AAD using the Lab Api up to specified number of
     * attempts. The primary reason for this overload is that device objects take some time to sync
     * in the directory and so a delete attempt made right after registration may not be successful,
     * and multiple attempts to delete the record can result in eventual success.
     *
     * @param upn                             the upn of the owner of this device
     * @param deviceId                        the device id of the device to be deleted
     * @param numDeleteAttemptsRemaining      the number times Lab Api should attempt to delete the device
     * @param waitTimeBeforeEachDeleteAttempt the amount of time to wait before each delete attempt
     * @return a boolean indicated if device has been deleted
     * @throws LabApiException if an error occurs while trying to delete the device
     */
    boolean deleteDevice(String upn, String deviceId, int numDeleteAttemptsRemaining, long waitTimeBeforeEachDeleteAttempt) throws LabApiException;
}
