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

        final boolean isMultiResourceCapable = MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(
                accountToSave.getAuthorityType()
        );

        Logger.info(
                TAG + methodName,
                "isMultiResourceCapable? [" + isMultiResourceCapable + "]"
        );

        if (isMultiResourceCapable) {
            // AAD v1 & v2 support multi-resource refresh tokens, allowing us to use
            // a single refresh token to service all of an account's requests.
            // To ensure that only one refresh token is maintained for an account,
            // refresh tokens are cleared from the cache for the account which is about to be
            // saved (in the event that there was already a refresh token in the cache)
            final int refreshTokensRemoved = removeCredentialsOfTypeForAccount(
                    accountToSave.getEnvironment(),
                    refreshTokenToSave.getClientId(),
                    CredentialType.RefreshToken,
                    accountToSave
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

    @Override
    public boolean removeAccount(final String environment,
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
            return false;
        }

        // Remove this user's AccessToken, RefreshToken, IdToken, and Account entries
        int atsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.AccessToken,
                targetAccount
        );
        int rtsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.RefreshToken,
                targetAccount
        );
        int idsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.IdToken,
                targetAccount
        );

        final boolean accountRemoved = mAccountCredentialCache.removeAccount(targetAccount);

        final String[][] logInfo = new String[][]{
                {"Access tokens", String.valueOf(atsRemoved)},
                {"Refresh tokens", String.valueOf(rtsRemoved)},
                {"Id tokens", String.valueOf(idsRemoved)},
                {"Accounts", accountRemoved ? "1" : "0"}
        };

        for (final String[] tuple : logInfo) {
            com.microsoft.identity.common.internal.logging.Logger.info(
                    TAG + methodName,
                    tuple[0] + " removed: [" + tuple[1] + "]"
            );
        }

        return accountRemoved;
    }

    /**
     * Removes Credentials of the supplied type for the supplied Account.
     *
     * @param credentialType The type of Credential to remove.
     * @param targetAccount  The target Account whose Credentials should be removed.
     * @return The number of Credentials removed.
     */
    private int removeCredentialsOfTypeForAccount(
            @NonNull final String environment, // 'authority host'
            @NonNull final String clientId,
            @NonNull final CredentialType credentialType,
            @NonNull final AccountRecord targetAccount) {
        int credentialsRemoved = 0;

        // Query it for Credentials matching the supplied targetAccount
        final List<Credential> credentialsToRemove =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        targetAccount.getHomeAccountId(),
                        environment,
                        credentialType,
                        clientId,
                        targetAccount.getRealm(), // wildcard (*) realm
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

    private void deleteAccessTokensWithIntersectingScopes(final AccessTokenRecord referenceToken) {
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

    private boolean scopesIntersect(final AccessTokenRecord token1, final AccessTokenRecord token2) {
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
