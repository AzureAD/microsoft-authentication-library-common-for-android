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
package com.microsoft.identity.common.internal.cache;

import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;

import java.util.List;

/**
 * Account & Credential cache interface.
 */
public interface IAccountCredentialCache {

    /**
     * Saves the supplied Account in the cache.
     *
     * @param account The Account to save.
     */
    void saveAccount(final Account account);

    /**
     * Saves the supplied Credential in the cache.
     *
     * @param credential The Credential to save.
     */
    void saveCredential(final Credential credential);

    /**
     * Gets the Account saved for the supplied cache key.
     *
     * @param cacheKey The cache key to use when consulting the cache.
     * @return The saved Account or null if no cache entry exists.
     */
    Account getAccount(final String cacheKey);

    /**
     * Gets the Credential saved for the supplied cache key.
     *
     * @param cacheKey The cache key to use when consulting the cache.
     * @return The saved Credential or null if no cache entry exists.
     */
    Credential getCredential(final String cacheKey);

    /**
     * Returns all of the Accounts saved in the cache.
     *
     * @return The saved Accounts.
     */
    List<Account> getAccounts();

    /**
     * Returns all of the Accounts matching the supplied criteria.
     *
     * @param homeAccountId The homeAccountId used to match Account cache keys.
     * @param environment   The environment used to match Account cache keys.
     * @param realm         The realm used to match Account cache keys.
     * @return A List of Accounts matching the supplied criteria.
     */
    List<Account> getAccountsFilteredBy(
            @Nullable final String homeAccountId,
            final String environment,
            @Nullable final String realm
    );

    /**
     * Returns all of the Credentials saved in the cache.
     *
     * @return The saved Credentials.
     */
    List<Credential> getCredentials();

    /**
     * Returns all of the Credentials matching the supplied criteria.
     *
     * @param homeAccountId  The homeAccountId used to match Credential cache keys.
     * @param environment    The environment used to match Credential cache keys.
     * @param credentialType The sought CredentialType.
     * @param clientId       The clientId used to match Credential cache keys.
     * @param realm          The realm used to match Credential cache keys.
     * @param target         The target used to match Credential cache keys.
     * @return A List of Credentials matching the supplied criteria.
     */
    List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            @Nullable final String realm,
            @Nullable final String target
    );

    /**
     * Removes the supplied Account from the cache.
     *
     * @param accountToRemove The Account to delete.
     * @return True if the Account was deleted. False otherwise.
     */
    boolean removeAccount(final Account accountToRemove);

    /**
     * Removes the supplied Credential from the cache.
     *
     * @param credentialToRemove The Credential to delete.
     * @return True if the Credential was deleted. False otherwise.
     */
    boolean removeCredential(final Credential credentialToRemove);

    /**
     * Clear the contents of the cache.
     */
    void clearAll();

}
