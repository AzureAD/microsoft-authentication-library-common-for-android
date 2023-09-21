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

import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.UserType;

/**
 * An interface describing the properties that should be available on an Account provided by the
 * Lab Api.
 */
public interface ILabAccount {

    /**
     * Get the username (UPN) of this account.
     *
     * @return a String representing the account's username
     */
    String getUsername();

    /**
     * Get the password used for signing in with this account.
     *
     * @return a String representing the account's password
     */
    String getPassword();

    /**
     * Get the {@link UserType} of this account.
     *
     * @return the {@link UserType} representing account's user type
     */
    UserType getUserType();

    /**
     * Get the home tenant id of this account.
     *
     * @return a String representing the account's home tenant id
     */
    String getHomeTenantId();

    /**
     * A client id that can be used alongside this account to get a token.
     *
     * @return a String representing a client id
     */
    String getAssociatedClientId();

    /**
     * Get authority (cloud URL) that can be used for this lab account.
     *
     * @return a String representing the authority host for this lab account
     */
    String getAuthority();

    String getAzureEnvironment();
}
