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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.microsoft.identity.common.exception.ErrorStrings.ACCOUNT_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.exception.ErrorStrings.CREDENTIAL_IS_SCHEMA_NONCOMPLIANT;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MsalOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends TokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends com.microsoft.identity.common.internal.providers.oauth2.RefreshToken>
        extends OAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse>
        implements IShareSingleSignOnState<GenericAccount, GenericRefreshToken> {

    private static final String TAG = MsalOAuth2TokenCache.class.getSimpleName();

    private IAccountCredentialCache mAccountCredentialCache;

    private final IAccountCredentialAdapter<
            GenericOAuth2Strategy,
            GenericAuthorizationRequest,
            GenericTokenResponse,
            GenericAccount,
            GenericRefreshToken> mAccountCredentialAdapter;

    /**
     * Constructor of MsalOAuth2TokenCache.
     *
     * @param context                  Context
     * @param accountCredentialCache   IAccountCredentialCache
     * @param accountCredentialAdapter IAccountCredentialAdapter
     */
    public MsalOAuth2TokenCache(final Context context,
                                final IAccountCredentialCache accountCredentialCache,
                                final IAccountCredentialAdapter<
                                        GenericOAuth2Strategy,
                                        GenericAuthorizationRequest,
                                        GenericTokenResponse,
                                        GenericAccount,
                                        GenericRefreshToken> accountCredentialAdapter) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        mAccountCredentialCache = accountCredentialCache;
        mAccountCredentialAdapter = accountCredentialAdapter;
    }

    @Override
    public ICacheRecord save(@NonNull final GenericOAuth2Strategy oAuth2Strategy,
                             @NonNull final GenericAuthorizationRequest request,
                             @NonNull final GenericTokenResponse response) throws ClientException {
        final String methodName = ":save";
        // Create the Account
        final AccountRecord accountToSave =
                mAccountCredentialAdapter.createAccount(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the AccessToken
        final AccessTokenRecord accessTokenToSave =
                mAccountCredentialAdapter.createAccessToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the RefreshToken
        final RefreshTokenRecord refreshTokenToSave =
                mAccountCredentialAdapter.createRefreshToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the IdToken
        final IdTokenRecord idTokenToSave =
                mAccountCredentialAdapter.createIdToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Check that everything we're about to save is schema-compliant...
        validateCacheArtifacts(
                accountToSave,
                accessTokenToSave,
                refreshTokenToSave,
                idTokenToSave
        );

        final boolean isFamilyRefreshToken = !StringExtensions.isNullOrBlank(
                refreshTokenToSave.getFamilyId()
        );

        Logger.info(
                TAG + methodName,
                "isFamilyRefreshToken? [" + isFamilyRefreshToken + "]"
        );

        final boolean isMultiResourceCapable = MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(
                accountToSave.getAuthorityType()
        );

        Logger.info(
                TAG + methodName,
                "isMultiResourceCapable? [" + isMultiResourceCapable + "]"
        );

        if (isFamilyRefreshToken || isMultiResourceCapable) {
            // AAD v1 & v2 support multi-resource refresh tokens, allowing us to use
            // a single refresh token to service all of an account's requests.
            // To ensure that only one refresh token is maintained for an account,
            // refresh tokens are cleared from the cache for the account which is about to be
            // saved (in the event that there was already a refresh token in the cache)

            // AAD v1 & v2 also support the use of family refresh tokens (FRTs).
            // FRTs allow us to think of 1st party clients as having a shared clientId, though
            // this isn't *actually* the case. Basically, 1st party tokens in "the family"
            // are allowed to use one another's MRRTs so long as they match the current account.
            final int refreshTokensRemoved = removeCredentialsOfTypeForAccount(
                    accountToSave.getEnvironment(),
                    isFamilyRefreshToken
                            // Delete all RTs, irrespective of clientId.
                            // Please note: this mechanism relies on there being a logically
                            // separate cache for FOCI inside the broker. If more families are
                            // added in the future (eg "foci" : "2") this logic will need to be
                            // modified so that the deletion is scoped to only delete RTs in a
                            // provided family. This is unimplemented for now, as it complicates the
                            // api and has only a marginal chance of ever being needed.
                            // Basically, FOCI is more like true/false than a real "id".

                            // If this cache is running in standalone (no-broker) mode,
                            // then we can think of this call as saying 'delete all RTs in the cache
                            // relative to the current account and for all client ids'. Since
                            // standalone mode only ever serves a single client id, this should be
                            // OK for now. If TSL support comes later, this approach may need to be
                            // reevaluated.
                            ? null
                            // Delete all RTs relative to this client ID
                            : refreshTokenToSave.getClientId(),
                    CredentialType.RefreshToken,
                    accountToSave,
                    true
            );

            Logger.info(
                    TAG + methodName,
                    "Refresh tokens removed: [" + refreshTokensRemoved + "]"
            );

            if (refreshTokensRemoved > 1) {
                Logger.warn(
                        TAG + methodName,
                        "Multiple refresh tokens found for Account."
                );
            }
        }

        // Save the Account and Credentials...
        saveAccounts(accountToSave);
        saveCredentials(accessTokenToSave, refreshTokenToSave, idTokenToSave);

        final CacheRecord result = new CacheRecord();
        result.setAccount(accountToSave);
        result.setAccessToken(accessTokenToSave);
        result.setRefreshToken(refreshTokenToSave);
        result.setIdToken(idTokenToSave);

        return result;
    }

    @Override
    public ICacheRecord save(@NonNull final AccountRecord accountToSave,
                             @NonNull final IdTokenRecord idTokenToSave) {
        final String methodName = ":save";

        Logger.verbose(
                TAG + methodName,
                "Importing AccountRecord, IdTokenRecord (direct)"
        );

        // Validate the incoming artifacts
        final boolean isAccountCompliant = isAccountSchemaCompliant(accountToSave);
        final boolean isIdTokenCompliant = isIdTokenSchemaCompliant(idTokenToSave);

        final CacheRecord result = new CacheRecord();

        if (!(isAccountCompliant && isIdTokenCompliant)) {
            String nonCompliantCredentials = "[";

            if (!isAccountCompliant) {
                nonCompliantCredentials += "(Account)";
            }

            if (!isIdTokenCompliant) {
                nonCompliantCredentials += "(ID)";
            }

            nonCompliantCredentials += "]";

            Logger.warn(
                    TAG + methodName,
                    "Skipping persistence of non-compliant credentials: "
                            + nonCompliantCredentials
            );
        } else {
            // Save the inputs
            saveAccounts(accountToSave);
            saveCredentials(idTokenToSave);

            // Set them as the result outputs
            result.setAccount(accountToSave);
            result.setIdToken(idTokenToSave);
        }

        return result;
    }

    @Override
    public ICacheRecord loadByFamilyId(@Nullable final String clientId,
                                       @Nullable final String target,
                                       @NonNull final AccountRecord accountRecord) {
        final String methodName = ":loadByFamilyId";

        final String familyId = "1";

        Logger.verbose(
                TAG + methodName,
                "ClientId[" + clientId + ", " + familyId + "]"
        );

        ICacheRecord result = null;

        // Try to find a 'perfect match' if possible (clientId & target match)
        // If no perfect match, fall back on any RT for this app (clientId but no target)
        if (null != clientId) {
            result = load(clientId, target, accountRecord);

            // A result was found... therefore the familyId will be ignored...
            Logger.warn(
                    TAG + methodName,
                    "Credentials located for client id. Skipping family id check."
            );
        }

        // If there is no RT for this app, try to find any RT in the family (family id ONLY)
        if (null == result || null == result.getRefreshToken()) {
            Logger.warn(
                    TAG + methodName,
                    "Matching RT could not be found. Searching for compatible FRT."
            );

            final List<Credential> allCredentials = mAccountCredentialCache.getCredentials();
            // The following fields must match:
            // - environment
            // - home_account_id
            // - credential_type == RT

            // The following fields do not matter:
            // - clientId doesn't matter (FRT)
            // - target doesn't matter (FRT)
            // - realm doesn't matter (MRRT)

            final List<RefreshTokenRecord> allRefreshTokens = new ArrayList<>();

            // First, filter down to only the refresh tokens...
            for (final Credential credential : allCredentials) {
                if (credential instanceof RefreshTokenRecord) {
                    allRefreshTokens.add((RefreshTokenRecord) credential);
                }
            }

            Logger.info(
                    TAG + methodName,
                    "Found [" + allRefreshTokens.size() + "] RTs"
            );

            // Iterate over those refresh tokens and see if any are in the family...
            final List<RefreshTokenRecord> familyRefreshTokens = new ArrayList<>();

            for (final RefreshTokenRecord refreshToken : allRefreshTokens) {
                if (refreshToken.getFamilyId().equals(familyId)) {
                    familyRefreshTokens.add(refreshToken);
                }
            }

            Logger.info(
                    TAG + methodName,
                    "Found [" + familyRefreshTokens.size() + "] foci RTs"
            );

            // Iterate over the family refresh tokens and filter for the current environment...
            final List<RefreshTokenRecord> familyRtsForEnvironment = new ArrayList<>();

            for (final RefreshTokenRecord familyRefreshToken : familyRefreshTokens) {
                if (familyRefreshToken.getEnvironment().equals(accountRecord.getEnvironment())) {
                    familyRtsForEnvironment.add(familyRefreshToken);
                }
            }

            Logger.info(
                    TAG + methodName,
                    "Found [" + familyRtsForEnvironment.size() + "] foci RTs"
            );

            IdTokenRecord idTokenRecord = null;
            AccessTokenRecord accessTokenRecord = null;

            if (null != result) {
                // If our first call yielded an id or access token, bring that result 'forward'
                // and return it with the newly-found FRT... The onus is on the caller to check
                // if the AT is expired or not...
                idTokenRecord = result.getIdToken();
                accessTokenRecord = result.getAccessToken();
            }

            // Filter for the current user...
            result = new CacheRecord();
            ((CacheRecord) result).setAccount(accountRecord);

            for (final RefreshTokenRecord familyRefreshToken : familyRtsForEnvironment) {
                if (familyRefreshToken.getHomeAccountId().equals(accountRecord.getHomeAccountId())) {
                    Logger.verbose(
                            TAG + methodName,
                            "Compatible FOCI token found."
                    );

                    ((CacheRecord) result).setRefreshToken(familyRefreshToken);
                    ((CacheRecord) result).setIdToken(idTokenRecord);
                    ((CacheRecord) result).setAccessToken(accessTokenRecord);

                    break;
                }
            }
        }

        return result;
    }

    @Override
    public ICacheRecord load(@NonNull final String clientId,
                             @Nullable final String target,
                             @NonNull final AccountRecord account) {
        final boolean isMultiResourceCapable = MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(
                account.getAuthorityType()
        );

        // Load the AccessTokens
        final List<Credential> accessTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                CredentialType.AccessToken,
                clientId,
                account.getRealm(),
                target
        );

        // Load the RefreshTokens
        final List<Credential> refreshTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                CredentialType.RefreshToken,
                clientId,
                isMultiResourceCapable
                        ? null // wildcard (*)
                        : account.getRealm(),
                isMultiResourceCapable
                        ? null // wildcard (*)
                        : target
        );

        // Load the IdTokens
        final List<Credential> idTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                CredentialType.IdToken,
                clientId,
                account.getRealm(),
                null // wildcard (*)
        );

        final CacheRecord result = new CacheRecord();
        result.setAccount(account);
        result.setAccessToken(accessTokens.isEmpty() ? null : (AccessTokenRecord) accessTokens.get(0));
        result.setRefreshToken(refreshTokens.isEmpty() ? null : (RefreshTokenRecord) refreshTokens.get(0));
        result.setIdToken(idTokens.isEmpty() ? null : (IdTokenRecord) idTokens.get(0));

        return result;
    }

    @Override
    public boolean removeCredential(final Credential credential) {
        final String methodName = ":removeCredential";
        Logger.info(
                TAG + methodName,
                "Removing credential..."
        );
        Logger.infoPII(
                TAG + methodName,
                "ClientId: [" + credential.getClientId() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "CredentialType: [" + credential.getCredentialType() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "CachedAt: [" + credential.getCachedAt() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "Environment: [" + credential.getEnvironment() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "HomeAccountId: [" + credential.getHomeAccountId() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "IsExpired?: [" + credential.isExpired() + "]"
        );
        return mAccountCredentialCache.removeCredential(credential);
    }

    @Override
    public AccountRecord getAccount(@Nullable final String environment,
                                    @NonNull final String clientId,
                                    @NonNull final String homeAccountId,
                                    @Nullable final String realm) {
        final String methodName = ":getAccount";

        Logger.infoPII(
                TAG + methodName,
                "Environment: [" + environment + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "ClientId: [" + clientId + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "HomeAccountId: [" + homeAccountId + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "Realm: [" + realm + "]"
        );

        final List<AccountRecord> allAccounts = getAccounts(environment, clientId);

        Logger.info(
                TAG + methodName,
                "Found " + allAccounts.size() + " accounts"
        );

        // Return the sought Account matching the supplied homeAccountId and realm, if applicable
        for (final AccountRecord account : allAccounts) {
            if (homeAccountId.equals(account.getHomeAccountId())
                    && (null == realm || realm.equals(account.getRealm()))) {
                return account;
            }
        }

        Logger.warn(
                TAG + methodName,
                "No matching account found."
        );

        return null;
    }

    @Override
    public AccountRecord getAccountWithLocalAccountId(@Nullable String environment,
                                                      @NonNull String clientId,
                                                      @NonNull String localAccountId) {
        final String methodName = ":getAccountWithLocalAccountId";

        final List<AccountRecord> accounts = getAccounts(environment, clientId);

        Logger.infoPII(
                TAG + methodName,
                "LocalAccountId: [" + localAccountId + "]"
        );

        for (final AccountRecord account : accounts) {
            if (localAccountId.equals(account.getLocalAccountId())) {
                return account;
            }
        }

        return null;
    }

    @Override
    public List<AccountRecord> getAccounts(@Nullable final String environment,
                                           @NonNull final String clientId) {
        final String methodName = ":getAccounts";

        Logger.infoPII(
                TAG + methodName,
                "Environment: [" + environment + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "ClientId: [" + clientId + "]"
        );

        final List<AccountRecord> accountsForThisApp = new ArrayList<>();

        // Get all of the Accounts for this environment
        final List<AccountRecord> accountsForEnvironment =
                mAccountCredentialCache.getAccountsFilteredBy(
                        null, // wildcard (*) homeAccountId
                        environment,
                        null // wildcard (*) realm
                );

        Logger.info(
                TAG + methodName,
                "Found " + accountsForEnvironment.size() + " accounts for this environment"
        );

        // Grab the Credentials for this app...
        final List<Credential> appCredentials =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        null, // homeAccountId
                        environment,
                        CredentialType.IdToken,
                        clientId,
                        null, // realm
                        null // target
                );

        // For each Account with an associated RT, add it to the result List...
        for (final AccountRecord account : accountsForEnvironment) {
            if (accountHasCredential(account, appCredentials)) {
                accountsForThisApp.add(account);
            }
        }

        Logger.info(
                TAG + methodName,
                "Found " + accountsForThisApp.size() + " accounts for this clientId"
        );

        return Collections.unmodifiableList(accountsForThisApp);
    }

    /**
     * Evaluates the supplied list of Credentials. Returns true if he provided Account
     * 'owns' any one of these tokens.
     *
     * @param account        The Account whose credential ownership should be evaluated.
     * @param appCredentials The Credentials to evaluate.
     * @return True, if this Account has Credentials. False otherwise.
     */
    private boolean accountHasCredential(@NonNull final AccountRecord account,
                                         @NonNull final List<Credential> appCredentials) {
        final String methodName = ":accountHasCredential";

        final String accountHomeId = account.getHomeAccountId();
        final String accountEnvironment = account.getEnvironment();

        Logger.infoPII(
                TAG + methodName,
                "HomeAccountId: [" + accountHomeId + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "Environment: [" + accountEnvironment + "]"
        );

        for (final Credential credential : appCredentials) {
            if (accountHomeId.equals(credential.getHomeAccountId())
                    && accountEnvironment.equals(credential.getEnvironment())) {
                Logger.info(
                        TAG + methodName,
                        "Credentials located for account."
                );
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the specified Account or Accounts from the cache.
     * <p>
     * Note: if realm is passed as null, all tokens and AccountRecords associated to the
     * provided homeAccountId will be deleted. If a realm is provided, then the deletion is
     * restricted to only those AccountRecords and Credentials in that realm (tenant).
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @param realm         The tenant id of the targeted Account (if applicable).
     * @return
     */
    @Override
    public AccountDeletionRecord removeAccount(final String environment,
                                               final String clientId,
                                               final String homeAccountId,
                                               @Nullable final String realm) {
        final String methodName = ":removeAccount";

        Logger.infoPII(
                TAG + methodName,
                "Environment: [" + environment + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "ClientId: [" + clientId + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "HomeAccountId: [" + homeAccountId + "]"
        );

        Logger.infoPII(
                TAG + methodName,
                "Realm: [" + realm + "]"
        );

        final AccountRecord targetAccount;
        if (null == environment
                || null == clientId
                || null == homeAccountId
                || null == (targetAccount =
                getAccount(
                        environment,
                        clientId,
                        homeAccountId,
                        realm
                ))) {
            return new AccountDeletionRecord(null);
        }

        // If no realm is provided, remove the Account/Credentials from all realms.
        final boolean isRealmAgnostic = (null == realm);

        Logger.info(
                TAG + methodName,
                "IsRealmAgnostic? " + isRealmAgnostic
        );

        // Remove this user's AccessToken, RefreshToken, IdToken, and Account entries
        final int atsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.AccessToken,
                targetAccount,
                isRealmAgnostic
        );

        final int rtsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.RefreshToken,
                targetAccount,
                isRealmAgnostic
        );

        final int idsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.IdToken,
                targetAccount,
                isRealmAgnostic
        );

        final List<AccountRecord> deletedAccounts = new ArrayList<>();

        if (isRealmAgnostic) {
            // Remove all Accounts associated with this home_account_id...
            final List<AccountRecord> accountsToRemove = mAccountCredentialCache.getAccountsFilteredBy(
                    homeAccountId,
                    environment,
                    null // wildcard (*) realm
            );

            for (final AccountRecord accountToRemove : accountsToRemove) {
                if (mAccountCredentialCache.removeAccount(accountToRemove)) {
                    deletedAccounts.add(accountToRemove);
                }
            }
        } else {
            // Remove only the target Account
            if (mAccountCredentialCache.removeAccount(targetAccount)) {
                deletedAccounts.add(targetAccount);
            }
        }

        final String[][] logInfo = new String[][]{
                {"Access tokens", String.valueOf(atsRemoved)},
                {"Refresh tokens", String.valueOf(rtsRemoved)},
                {"Id tokens", String.valueOf(idsRemoved)},
                {"Accounts", String.valueOf(deletedAccounts.size())}
        };

        for (final String[] tuple : logInfo) {
            com.microsoft.identity.common.internal.logging.Logger.info(
                    TAG + methodName,
                    tuple[0] + " removed: [" + tuple[1] + "]"
            );
        }

        return new AccountDeletionRecord(deletedAccounts);
    }

    /**
     * Removes Credentials of the supplied type for the supplied Account.
     *
     * @param environment    Entity which issued the token represented as a host.
     * @param clientId       The clientId of the target app.
     * @param credentialType The type of Credential to remove.
     * @param targetAccount  The target Account whose Credentials should be removed.
     * @param realmAgnostic  True if the specified action should be completed irrespective of realm.
     * @return The number of Credentials removed.
     */
    private int removeCredentialsOfTypeForAccount(
            @NonNull final String environment, // 'authority host'
            @Nullable final String clientId,
            @NonNull final CredentialType credentialType,
            @NonNull final AccountRecord targetAccount,
            boolean realmAgnostic) {
        int credentialsRemoved = 0;

        // Query it for Credentials matching the supplied targetAccount
        final List<Credential> credentialsToRemove =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        targetAccount.getHomeAccountId(),
                        environment,
                        credentialType,
                        clientId,
                        realmAgnostic
                                ? null // wildcard (*) realm
                                : targetAccount.getRealm(),
                        null // wildcard (*) target
                );

        for (final Credential credentialToRemove : credentialsToRemove) {
            if (mAccountCredentialCache.removeCredential(credentialToRemove)) {
                credentialsRemoved++;
            }
        }

        return credentialsRemoved;
    }

    private void saveAccounts(final AccountRecord... accounts) {
        for (final AccountRecord account : accounts) {
            mAccountCredentialCache.saveAccount(account);
        }
    }

    private void saveCredentials(final Credential... credentials) {
        for (final Credential credential : credentials) {

            if (credential instanceof AccessTokenRecord) {
                deleteAccessTokensWithIntersectingScopes((AccessTokenRecord) credential);
            }

            mAccountCredentialCache.saveCredential(credential);
        }
    }

    /**
     * Validates that the supplied artifacts are schema-compliant and OK to write to the cache.
     *
     * @param accountToSave      The {@link AccountRecord} to save.
     * @param accessTokenToSave  The {@link AccessTokenRecord} to save or null. Null params are assumed
     *                           valid; this condition supports the SSO case.
     * @param refreshTokenToSave The {@link RefreshTokenRecord}
     *                           to save.
     * @param idTokenToSave      The {@link IdTokenRecord} to save.
     * @throws ClientException If any of the supplied artifacts are non schema-compliant.
     */
    private void validateCacheArtifacts(
            @NonNull final AccountRecord accountToSave,
            final AccessTokenRecord accessTokenToSave,
            @NonNull final RefreshTokenRecord refreshTokenToSave,
            @NonNull final IdTokenRecord idTokenToSave) throws ClientException {
        final String methodName = ":validateCacheArtifacts";
        Logger.info(
                TAG + methodName,
                "Validating cache artifacts..."
        );

        final boolean isAccountCompliant = isAccountSchemaCompliant(accountToSave);
        final boolean isAccessTokenCompliant = null == accessTokenToSave || isAccessTokenSchemaCompliant(accessTokenToSave);
        final boolean isRefreshTokenCompliant = isRefreshTokenSchemaCompliant(refreshTokenToSave);
        final boolean isIdTokenCompliant = isIdTokenSchemaCompliant(idTokenToSave);

        if (!isAccountCompliant) {
            throw new ClientException(ACCOUNT_IS_SCHEMA_NONCOMPLIANT);
        }

        if (!(isAccessTokenCompliant
                && isRefreshTokenCompliant
                && isIdTokenCompliant)) {
            String nonCompliantCredentials = "[";

            if (!isAccessTokenCompliant) {
                nonCompliantCredentials += "(AT)";
            }

            if (!isRefreshTokenCompliant) {
                nonCompliantCredentials += "(RT)";
            }

            if (!isIdTokenCompliant) {
                nonCompliantCredentials += "(ID)";
            }

            nonCompliantCredentials += "]";

            throw new ClientException(
                    CREDENTIAL_IS_SCHEMA_NONCOMPLIANT,
                    nonCompliantCredentials
            );
        }
    }

    private void deleteAccessTokensWithIntersectingScopes(
            final AccessTokenRecord referenceToken) {
        final String methodName = "deleteAccessTokensWithIntersectingScopes";

        final List<Credential> accessTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                referenceToken.getHomeAccountId(),
                referenceToken.getEnvironment(),
                CredentialType.AccessToken,
                referenceToken.getClientId(),
                referenceToken.getRealm(),
                null // Wildcard - delete anything that matches...
        );

        Logger.verbose(
                TAG + ":" + methodName,
                "Inspecting " + accessTokens.size() + " accessToken[s]."
        );

        for (final Credential accessToken : accessTokens) {
            if (scopesIntersect(referenceToken, (AccessTokenRecord) accessToken)) {
                Logger.infoPII(TAG + ":" + methodName, "Removing credential: " + accessToken);
                mAccountCredentialCache.removeCredential(accessToken);
            }
        }
    }

    private boolean scopesIntersect(final AccessTokenRecord token1,
                                    final AccessTokenRecord token2) {
        final String methodName = "scopesIntersect";

        final Set<String> token1Scopes = scopesAsSet(token1);
        final Set<String> token2Scopes = scopesAsSet(token2);

        boolean result = false;
        for (final String scope : token2Scopes) {
            if (token1Scopes.contains(scope)) {
                Logger.info(TAG + ":" + methodName, "Scopes intersect.");
                Logger.infoPII(
                        TAG + ":" + methodName,
                        token1Scopes.toString() + " contains [" + scope + "]"
                );
                result = true;
                break;
            }
        }

        return result;
    }

    private Set<String> scopesAsSet(final AccessTokenRecord token) {
        final Set<String> scopeSet = new HashSet<>();
        final String scopeString = token.getTarget();

        if (!StringExtensions.isNullOrBlank(scopeString)) {
            final String[] scopeArray = scopeString.split("\\s+");
            scopeSet.addAll(Arrays.asList(scopeArray));
        }

        return scopeSet;
    }

    private static boolean isSchemaCompliant(final Class<?> clazz, final String[][] params) {
        final String methodName = "isSchemaCompliant";

        boolean isCompliant = true;
        for (final String[] param : params) {
            isCompliant = isCompliant && !StringExtensions.isNullOrBlank(param[1]);
        }

        if (!isCompliant) {
            Logger.warn(
                    TAG + ":" + methodName,
                    clazz.getSimpleName() + " does not contain all required fields."
            );

            for (final String[] param : params) {
                Logger.warn(
                        TAG + ":" + methodName,
                        param[0] + " is null? [" + StringExtensions.isNullOrBlank(param[1]) + "]"
                );
            }
        }

        return isCompliant;
    }

    private boolean isAccountSchemaCompliant(@NonNull final AccountRecord account) {
        // Required fields...
        final String[][] params = new String[][]{
                {AccountRecord.SerializedNames.HOME_ACCOUNT_ID, account.getHomeAccountId()},
                {AccountRecord.SerializedNames.ENVIRONMENT, account.getEnvironment()},
                //TODO Need to fix the validation for realm for AAD IDP scenario.
                //{AccountRecord.SerializedNames.REALM, account.getRealm()},
                {AccountRecord.SerializedNames.LOCAL_ACCOUNT_ID, account.getLocalAccountId()},
                {AccountRecord.SerializedNames.USERNAME, account.getUsername()},
                {AccountRecord.SerializedNames.AUTHORITY_TYPE, account.getAuthorityType()},
        };

        return isSchemaCompliant(account.getClass(), params);
    }

    private boolean isAccessTokenSchemaCompliant(@NonNull final AccessTokenRecord accessToken) {
        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, accessToken.getCredentialType()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, accessToken.getHomeAccountId()},
                //TODO Need to fix the validation for realm for AAD IDP scenario.
                //{AccessTokenRecord.SerializedNames.REALM, accessToken.getRealm()},
                {Credential.SerializedNames.ENVIRONMENT, accessToken.getEnvironment()},
                {Credential.SerializedNames.CLIENT_ID, accessToken.getClientId()},
                {AccessTokenRecord.SerializedNames.TARGET, accessToken.getTarget()},
                {Credential.SerializedNames.CACHED_AT, accessToken.getCachedAt()},
                {Credential.SerializedNames.EXPIRES_ON, accessToken.getExpiresOn()},
                {Credential.SerializedNames.SECRET, accessToken.getSecret()},
        };

        return isSchemaCompliant(accessToken.getClass(), params);
    }

    private boolean isRefreshTokenSchemaCompliant(
            @NonNull final RefreshTokenRecord refreshToken) {
        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, refreshToken.getCredentialType()},
                {Credential.SerializedNames.ENVIRONMENT, refreshToken.getEnvironment()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, refreshToken.getHomeAccountId()},
                {Credential.SerializedNames.CLIENT_ID, refreshToken.getClientId()},
                {Credential.SerializedNames.SECRET, refreshToken.getSecret()},
        };

        return isSchemaCompliant(refreshToken.getClass(), params);
    }

    private boolean isIdTokenSchemaCompliant(@NonNull final IdTokenRecord idToken) {
        final String[][] params = new String[][]{
                {Credential.SerializedNames.HOME_ACCOUNT_ID, idToken.getHomeAccountId()},
                {Credential.SerializedNames.ENVIRONMENT, idToken.getEnvironment()},
                //TODO Need to fix the validation for realm for AAD IDP scenario.
                //{IdTokenRecord.SerializedNames.REALM, idToken.getRealm()},
                {Credential.SerializedNames.CREDENTIAL_TYPE, idToken.getCredentialType()},
                {Credential.SerializedNames.CLIENT_ID, idToken.getClientId()},
                {Credential.SerializedNames.SECRET, idToken.getSecret()},
        };

        return isSchemaCompliant(idToken.getClass(), params);
    }

    @Override
    public boolean setSingleSignOnState(final GenericAccount account,
                                        final GenericRefreshToken refreshToken) {
        final String methodName = "setSingleSignOnState";

        try {
            final AccountRecord accountDto = mAccountCredentialAdapter.asAccount(account);
            final RefreshTokenRecord rt = mAccountCredentialAdapter.asRefreshToken(refreshToken);
            final IdTokenRecord idToken = mAccountCredentialAdapter.asIdToken(account, refreshToken);

            validateCacheArtifacts(
                    accountDto,
                    null,
                    rt,
                    idToken
            );

            mAccountCredentialCache.saveAccount(accountDto);
            mAccountCredentialCache.saveCredential(idToken);
            mAccountCredentialCache.saveCredential(rt);
            return true;
        } catch (ClientException e) {
            Logger.error(
                    TAG + ":" + methodName,
                    "",
                    new IllegalArgumentException(
                            "Cannot set SSO state. Invalid or inadequate Account and/or token provided. (See logs)",
                            e
                    )
            );
            return false;
        }
    }

    @Override
    public GenericRefreshToken getSingleSignOnState(final GenericAccount account) {
        throw new UnsupportedOperationException("Unimplemented!");
    }

}
