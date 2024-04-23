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
package com.microsoft.identity.common.java.cache;


import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.identity.common.java.exception.ErrorStrings.CREDENTIAL_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal.SCHEME_BEARER;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Sub class of {@link MsalCppOAuth2TokenCache} to add specific public api's required for MSAL CPP library.
 */
// Suppressing rawtype warnings due to the generic type OAuth2Strategy, AuthorizationRequest, IAccountCredentialAdapter, MsalCppOAuth2TokenCache and MsalOAuth2TokenCache
@SuppressWarnings(WarningType.rawtype_warning)
public class MsalCppOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends TokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends RefreshToken>
        extends MsalOAuth2TokenCache<
        GenericOAuth2Strategy,
        GenericAuthorizationRequest,
        GenericTokenResponse,
        GenericAccount,
        GenericRefreshToken> {

    private static final String TAG = MsalCppOAuth2TokenCache.class.getSimpleName();

    /**
     * Constructor of MsalOAuth2TokenCache.
     *
     * @param commonComponents         {@link IPlatformComponents}
     * @param accountCredentialCache   IAccountCredentialCache
     * @param accountCredentialAdapter IAccountCredentialAdapter
     */
    // Suppressing unchecked warnings due to casting of IAccountCredentialAdapter with the generics in the call to the constructor of parent class
    @SuppressWarnings(WarningType.unchecked_warning)
    private MsalCppOAuth2TokenCache(final IPlatformComponents commonComponents,
                                    final IAccountCredentialCache accountCredentialCache,
                                    final IAccountCredentialAdapter accountCredentialAdapter) {
        super(commonComponents, accountCredentialCache, accountCredentialAdapter);
    }

    /**
     * Factory method for creating an instance of MsalCppOAuth2TokenCache.
     *
     * @param platformComponents The Application Context
     * @return An instance of the MsalCppOAuth2TokenCache.
     */
    // Suppressing unchecked warning as the return type requiring generic parameter which is not provided
    @SuppressWarnings(WarningType.unchecked_warning)
    public static MsalCppOAuth2TokenCache create(@NonNull final IPlatformComponents platformComponents) {
        return create(platformComponents, false);
    }

    /**
     * Factory method for creating an instance of MsalCppOAuth2TokenCache.
     *
     * @param platformComponents The Application Context
     * @param useInMemoryCache Opt-in to caching layer that holds account and credential objects in memory
     * @return An instance of the MsalCppOAuth2TokenCache.
     */
    // Suppressing unchecked warning as the return type requiring generic parameter which is not provided
    @SuppressWarnings(WarningType.unchecked_warning)
    public static MsalCppOAuth2TokenCache create(@NonNull final IPlatformComponents platformComponents, boolean useInMemoryCache) {
        final MsalOAuth2TokenCache msalOAuth2TokenCache = MsalOAuth2TokenCache.create(platformComponents, useInMemoryCache);

        // Suppressing unchecked warnings due to the generic types not provided while creating object of MsalCppOAuth2TokenCache
        @SuppressWarnings(WarningType.unchecked_warning)
        MsalCppOAuth2TokenCache msalCppOAuth2TokenCache = new MsalCppOAuth2TokenCache(
                platformComponents,
                msalOAuth2TokenCache.getAccountCredentialCache(),
                msalOAuth2TokenCache.getAccountCredentialAdapter()
        );

        return msalCppOAuth2TokenCache;
    }

    public IAccountCredentialCache getAccountCredentialCache() {
        return super.getAccountCredentialCache();
    }

    /**
     * @param credentials       list of Credential which can include AccessTokenRecord, IdTokenRecord and RefreshTokenRecord.
     * @throws ClientException  If the supplied Account or Credential are null or schema invalid.
     */
    public synchronized void saveCredentials(@NonNull final Credential... credentials) throws ClientException {
        if (credentials.length == 0) {
            throw new ClientException("Credential array passed in is null or empty");
        }

        RefreshTokenRecord refreshTokenRecord = null;

        for (final Credential credential : credentials) {
            if (credential instanceof RefreshTokenRecord) {
                refreshTokenRecord = (RefreshTokenRecord) credential;
            }

            if (credential instanceof AccessTokenRecord
                    && !isAccessTokenSchemaCompliant((AccessTokenRecord) credential)) {
                throw new ClientException(
                        CREDENTIAL_IS_SCHEMA_NONCOMPLIANT,
                        "AT is missing a required property."
                );
            }
        }

        saveCredentialsInternal(credentials);
    }

    /**
     * API to save {@link AccountRecord}
     *
     * @param accountRecord : accountRecord to be saved.
     */
    public void saveAccountRecord(@NonNull final AccountRecord accountRecord) {
        getAccountCredentialCache().saveAccount(accountRecord);
    }

    /**
     * API to clear all cache.
     * Note: This method is intended to be only used for testing purposes.
     */
    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void clearCache() {
        getAccountCredentialCache().clearAll();
    }

    /**
     * API to inspect cache contents.
     * Note: This method is intended to be only used for testing purposes.
     *
     * @return A immutable List of Credentials contained in this cache.
     */
    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public List<Credential> getCredentials() {
        return Collections.unmodifiableList(
                getAccountCredentialCache().getCredentials()
        );
    }

    /**
     * Force remove an AccountRecord matching the supplied criteria.
     *
     * @param homeAccountId HomeAccountId of the Account.
     * @param environment   The Environment of the Account.
     * @param realm         The Realm of the Account.
     * @return An {@link AccountDeletionRecord} containing a receipt of the removed Accounts.
     * @throws ClientException
     */
    //@VisibleForTesting // private by default for production code
    public synchronized AccountDeletionRecord forceRemoveAccount(@NonNull final String homeAccountId,
                                                                 @Nullable final String environment,
                                                                 @Nullable final String realm) throws ClientException {
        validateNonNull(homeAccountId, "homeAccountId");

        final boolean mustMatchOnEnvironment = !StringUtil.isNullOrEmpty(environment);
        final boolean mustMatchOnRealm = !StringUtil.isNullOrEmpty(realm);

        final List<AccountRecord> removedAccounts = new ArrayList<>();

        for (final AccountRecord accountRecord : getAllAccounts()) {
            boolean matches = accountRecord.getHomeAccountId().equals(homeAccountId);

            if (mustMatchOnEnvironment) {
                matches = matches && accountRecord.getEnvironment().equals(environment);
            }

            if (mustMatchOnRealm) {
                matches = matches && accountRecord.getRealm().equals(realm);
            }

            if (matches) {
                // Delete the AccountRecord...
                final boolean accountRemoved = getAccountCredentialCache().removeAccount(accountRecord);

                if (accountRemoved) {
                    removedAccounts.add(accountRecord);
                }
            }
        }

        return new AccountDeletionRecord(removedAccounts);
    }

    /**
     * Method to remove Account matched with homeAccountId, environment and realm
     *
     * @param homeAccountId : HomeAccountId of the Account
     * @param environment   : Environment of the Account
     * @param realm         : Realm of the Account
     * @return {@link AccountDeletionRecord}
     */
    public synchronized AccountDeletionRecord removeAccount(@NonNull final String homeAccountId,
                                                            @NonNull final String environment,
                                                            @NonNull final String realm) throws ClientException {
        // TODO This API is potentially problematic for TFW/TFL...
        // Normally on Android, apps are 'sandboxed' such that each app has their own cache
        // and we don't have to worry about 1 app stomping on another's cache
        //
        // TFW/TFL however, "double stacked" their app registrations into a single binary
        // Such that calling removeAccount() will potentially remove the Account being used by
        // another app.
        //
        // This API assumes the *general* case where an app is single stacked. If special
        // accommodations need to come later for Teams then we can reevaluate the logic here.

        validateNonNull(homeAccountId, "homeAccountId");
        validateNonNull(environment, "environment");
        validateNonNull(realm, "realm");
        
        final String normalizedEnvironment = environment.equals("") ? null : environment;
        final String normalizedRealm = realm.equals("") ? null : realm;

        final List<Credential> credentials = getAccountCredentialCache().getCredentialsFilteredBy(
                homeAccountId,
                normalizedEnvironment,
                CredentialType.RefreshToken,
                null, //wildcard (*)
                null, //wildcard (*)
                null, //wildcard (*)
                normalizedRealm,
                null, //wildcard (*)
                SCHEME_BEARER
        );

        if (credentials != null && !credentials.isEmpty()) {
            // Get a client id to use for deletion
            final String clientId = credentials.get(0).getClientId();

            // Remove the account
            return removeAccount(
                    normalizedEnvironment,
                    clientId,
                    homeAccountId,
                    normalizedRealm,
                    CredentialType.AccessToken,
                    CredentialType.AccessToken_With_AuthScheme,
                    CredentialType.IdToken,
                    CredentialType.V1IdToken
            );
        } else {
            // Remove was called, but no RTs exist for the account. Force remove it.
            return forceRemoveAccount(homeAccountId, normalizedEnvironment, normalizedRealm);
        }
    }

    /**
     * Gets an immutable {@link List} of {@link AccountRecord} objects.
     */
    public List<AccountRecord> getAllAccounts() {
        return Collections.unmodifiableList(
                getAccountCredentialCache().getAccounts()
        );
    }

    /**
     * Method to get Account matched with homeAccountId, environment and realm
     *
     * @param homeAccountId : HomeAccountId of the Account
     * @param environment   : Environment of the Account
     * @param realm         : Realm of the Account
     * @return {@link AccountRecord}
     * @throws ClientException : throws ClientException if input validation fails
     */
    @Nullable
    public AccountRecord getAccount(@NonNull final String homeAccountId,
                                    @NonNull final String environment,
                                    @NonNull final String realm) throws ClientException {
        final String methodName = ":getAccount";

        validateNonNull(homeAccountId, "homeAccountId");
        validateNonNull(environment, "environment");
        validateNonNull(realm, "realm");

        final List<AccountRecord> accountRecords = getAccountCredentialCache()
                .getAccountsFilteredBy(homeAccountId, environment, realm);

        if (accountRecords == null || accountRecords.isEmpty()) {
            Logger.info(TAG + methodName,
                    "No account found for the passing in "
                            + "homeAccountId: " + homeAccountId
                            + " environment: " + environment
                            + " realm: " + realm
            );
            return null;
        }

        return accountRecords.get(0);
    }

}
