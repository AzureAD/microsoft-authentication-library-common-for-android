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
    LabAccount getLabAccount(LabQuery labQuery) throws LabApiException;

    /**
     * Loads existing account(s) from Lab Api based on the provided query.
     *
     * @param labQuery parameters that determine what kind of account(s) to fetch from lab
     * @return a list of {@link LabAccount} objects
     * @throws LabApiException if an error occurs while trying to fetch account(s) from lab
     */
    List<LabAccount> getLabAccounts(LabQuery labQuery) throws LabApiException;

    /**
     * Create and return a new temp AAD user using Lab Api.
     *
     * @param tempUserType the {@link TempUserType} of the user to create
     * @return a {@link LabAccount} object
     * @throws LabApiException if an error occurs while trying to fetch account from lab
     */
    LabAccount createTempAccount(TempUserType tempUserType) throws LabApiException;

    /**
     * Get the value of a secret from Lab Api. This primarily includes secrets like passwords for
     * accounts but may also be used for any other secret that the Lab has stored in their KeyVault.
     *
     * @param secretName the name (identifier) of the secret that should be loaded
     * @return a String containing the value of the secret
     * @throws LabApiException if an error occurs while trying to load secret from lab
     */
    String getSecret(String secretName) throws LabApiException;

    boolean resetPassword(String upn) throws LabApiException;
}
