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

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.microsoft.identity.common.exception.ErrorStrings.ACCOUNT_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.exception.ErrorStrings.CREDENTIAL_IS_SCHEMA_NONCOMPLIANT;

public class MsalOAuth2TokenCache
        extends OAuth2TokenCache<MicrosoftStsOAuth2Strategy, MicrosoftStsAuthorizationRequest, MicrosoftStsTokenResponse>
        implements IShareSingleSignOnState<MicrosoftAccount, MicrosoftRefreshToken> {

    private static final String TAG = MsalOAuth2TokenCache.class.getSimpleName();

    private List<IShareSingleSignOnState> mSharedSsoCaches; //NOPMD Suppressing PMD warning for unused variable
    private IAccountCredentialCache mAccountCredentialCache;

    private IAccountCredentialAdapter<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mAccountCredentialAdapter;

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
                                        MicrosoftStsOAuth2Strategy,
                                        MicrosoftStsAuthorizationRequest,
                                        MicrosoftStsTokenResponse,
                                        MicrosoftAccount,
                                        MicrosoftRefreshToken> accountCredentialAdapter) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        mAccountCredentialCache = accountCredentialCache;
        mSharedSsoCaches = new ArrayList<>();
        mAccountCredentialAdapter = accountCredentialAdapter;
    }

    /**
     * Constructor of MsalOAuth2TokenCache.
     *
     * @param context                  Context
     * @param accountCredentialCache   IAccountCredentialCache
     * @param accountCredentialAdapter IAccountCredentialAdapter
     * @param sharedSsoCaches          List<IShareSingleSignOnState>
     */
    public MsalOAuth2TokenCache(final Context context,
                                final IAccountCredentialCache accountCredentialCache,
                                final IAccountCredentialAdapter<
                                        MicrosoftStsOAuth2Strategy,
                                        MicrosoftStsAuthorizationRequest,
                                        MicrosoftStsTokenResponse,
                                        MicrosoftAccount,
                                        MicrosoftRefreshToken> accountCredentialAdapter,
                                final List<IShareSingleSignOnState> sharedSsoCaches) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        mAccountCredentialCache = accountCredentialCache;
        mSharedSsoCaches = sharedSsoCaches;
        mAccountCredentialAdapter = accountCredentialAdapter;
    }

    @Override
    public void saveTokens(
            final MicrosoftStsOAuth2Strategy oAuth2Strategy,
            final MicrosoftStsAuthorizationRequest request,
            final MicrosoftStsTokenResponse response) throws ClientException {
        // Create the Account
        final Account accountToSave =
                mAccountCredentialAdapter.createAccount(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the AccessToken
        final AccessToken accessTokenToSave =
                mAccountCredentialAdapter.createAccessToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the RefreshToken
        final com.microsoft.identity.common.internal.dto.RefreshToken refreshTokenToSave =
                mAccountCredentialAdapter.createRefreshToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the IdToken
        final IdToken idTokenToSave =
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

        // Save the Account and Credentials...
        saveAccounts(accountToSave);
        saveCredentials(accessTokenToSave, refreshTokenToSave, idTokenToSave);
    }

    @Override
    public Account getAccount(final String environment,
                              final String clientId,
                              final String homeAccountId) {
        final List<Account> allAccounts = getAccounts(environment, clientId);

        // Return the sought Account matching the supplied homeAccountId
        for (final Account account : allAccounts) {
            if (homeAccountId.equals(account.getHomeAccountId())) {
                return account;
            }
        }

        return null;
    }

    @Override
    public List<Account> getAccounts(final String environment,
                                     final String clientId) {
        final List<Account> accountsForThisApp = new ArrayList<>();

        // Get all of the Accounts for this environment
        final List<Account> accountsForEnvironment =
                mAccountCredentialCache.getAccountsFilteredBy(
                        null, // wildcard (*) homeAccountId
                        environment,
                        null // wildcard (*) realm
                );

        // Declare a List to hold the MicrosoftStsAccounts
        final List<Account> microsoftStsAccounts = new ArrayList<>();

        for (final Account account : accountsForEnvironment) {
            if (MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(account.getAuthorityType())) {
                microsoftStsAccounts.add(account);
            }
        }

        // Grab the Credentials for this app...
        final List<Credential> appCredentials =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        null, // homeAccountId
                        environment,
                        CredentialType.RefreshToken,
                        clientId,
                        null, // realm
                        null // target
                );
        // For each Account with an associated RT, add it to the result List...
        for (final Account account : microsoftStsAccounts) {
            if (accountHasToken(account, appCredentials)) {
                accountsForThisApp.add(account);
            }
        }

        return Collections.unmodifiableList(accountsForThisApp);

    }

    /**
     * Evaluates the supplied list of app credentials. Returns true if he provided Account
     * 'owns' any one of these tokens.
     *
     * @param account        The Account whose credential ownership should be evaluated.
     * @param appCredentials The Credentials to evaluate.
     * @return True, if this Account has Credentials. False otherwise.
     */
    private boolean accountHasToken(final Account account,
                                    final List<Credential> appCredentials) {
        final String accountHomeId = account.getHomeAccountId();
        final String accountEnvironment = account.getEnvironment();

        for (final Credential credential : appCredentials) {
            if (accountHomeId.equals(credential.getHomeAccountId())
                    && accountEnvironment.equals(credential.getEnvironment())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean removeAccount(final String environment,
                                 final String clientId,
                                 final String homeAccountId) {
        final String methodName = ":removeAccount";

        final Account targetAccount;
        if (null == environment
                || null == clientId
                || null == homeAccountId
                || null == (targetAccount =
                getAccount(
                        environment,
                        clientId,
                        homeAccountId
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
            @NonNull final Account targetAccount) {
        int credentialsRemoved = 0;

        // Query it for Credentials matching the supplied targetAccount
        final List<Credential> credentialsToRemove =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        targetAccount.getHomeAccountId(),
                        environment,
                        credentialType,
                        clientId,
                        null, // wildcard (*) realm
                        null // wildcard (*) target
                );

        for (final Credential credentialToRemove : credentialsToRemove) {
            if (mAccountCredentialCache.removeCredential(credentialToRemove)) {
                credentialsRemoved++;
            }
        }

        return credentialsRemoved;
    }

    private void saveAccounts(final Account... accounts) {
        for (final Account account : accounts) {
            mAccountCredentialCache.saveAccount(account);
        }
    }

    private void saveCredentials(final Credential... credentials) {
        for (final Credential credential : credentials) {

            if (credential instanceof AccessToken) {
                deleteAccessTokensWithIntersectingScopes((AccessToken) credential);
            }

            mAccountCredentialCache.saveCredential(credential);
        }
    }

    /**
     * Validates that the supplied artifacts are schema-compliant and OK to write to the cache.
     *
     * @param accountToSave      The {@link Account} to save.
     * @param accessTokenToSave  The {@link AccessToken} to save or null. Null params are assumed
     *                           valid; this condition supports the SSO case.
     * @param refreshTokenToSave The {@link com.microsoft.identity.common.internal.dto.RefreshToken}
     *                           to save.
     * @param idTokenToSave      The {@link IdToken} to save.
     * @throws ClientException If any of the supplied artifacts are non schema-compliant.
     */
    private void validateCacheArtifacts(
            @NonNull final Account accountToSave,
            final AccessToken accessTokenToSave,
            @NonNull final com.microsoft.identity.common.internal.dto.RefreshToken refreshTokenToSave,
            @NonNull final IdToken idTokenToSave) throws ClientException {
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

    private void deleteAccessTokensWithIntersectingScopes(final AccessToken referenceToken) {
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
            if (scopesIntersect(referenceToken, (AccessToken) accessToken)) {
                Logger.infoPII(TAG + ":" + methodName, "Removing credential: " + accessToken);
                mAccountCredentialCache.removeCredential(accessToken);
            }
        }
    }

    private boolean scopesIntersect(final AccessToken token1, final AccessToken token2) {
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

    private Set<String> scopesAsSet(final AccessToken token) {
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

    private static boolean isAccountSchemaCompliant(@NonNull final Account account) {
        // Required fields...
        final String[][] params = new String[][]{
                {Account.SerializedNames.HOME_ACCOUNT_ID, account.getHomeAccountId()},
                {Account.SerializedNames.ENVIRONMENT, account.getEnvironment()},
                {Account.SerializedNames.REALM, account.getRealm()},
                {Account.SerializedNames.LOCAL_ACCOUNT_ID, account.getLocalAccountId()},
                {Account.SerializedNames.USERNAME, account.getUsername()},
                {Account.SerializedNames.AUTHORITY_TYPE, account.getAuthorityType()},
        };

        boolean isCompliant = isSchemaCompliant(account.getClass(), params);

        return isCompliant;
    }

    private static boolean isAccessTokenSchemaCompliant(@NonNull final AccessToken accessToken) {
        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, accessToken.getCredentialType()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, accessToken.getHomeAccountId()},
                {AccessToken.SerializedNames.REALM, accessToken.getRealm()},
                {Credential.SerializedNames.ENVIRONMENT, accessToken.getEnvironment()},
                {Credential.SerializedNames.CLIENT_ID, accessToken.getClientId()},
                {AccessToken.SerializedNames.TARGET, accessToken.getTarget()},
                {Credential.SerializedNames.CACHED_AT, accessToken.getCachedAt()},
                {Credential.SerializedNames.EXPIRES_ON, accessToken.getExpiresOn()},
                {Credential.SerializedNames.SECRET, accessToken.getSecret()},
        };

        boolean isValid = isSchemaCompliant(accessToken.getClass(), params);

        return isValid;
    }

    private static boolean isRefreshTokenSchemaCompliant(
            @NonNull final com.microsoft.identity.common.internal.dto.RefreshToken refreshToken) {
        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, refreshToken.getCredentialType()},
                {Credential.SerializedNames.ENVIRONMENT, refreshToken.getEnvironment()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, refreshToken.getHomeAccountId()},
                {Credential.SerializedNames.CLIENT_ID, refreshToken.getClientId()},
                {Credential.SerializedNames.SECRET, refreshToken.getSecret()},
        };

        boolean isValid = isSchemaCompliant(refreshToken.getClass(), params);

        return isValid;
    }

    private static boolean isIdTokenSchemaCompliant(@NonNull final IdToken idToken) {
        final String[][] params = new String[][]{
                {Credential.SerializedNames.HOME_ACCOUNT_ID, idToken.getHomeAccountId()},
                {Credential.SerializedNames.ENVIRONMENT, idToken.getEnvironment()},
                {IdToken.SerializedNames.REALM, idToken.getRealm()},
                {Credential.SerializedNames.CREDENTIAL_TYPE, idToken.getCredentialType()},
                {Credential.SerializedNames.CLIENT_ID, idToken.getClientId()},
                {Credential.SerializedNames.SECRET, idToken.getSecret()},
        };

        boolean isValid = isSchemaCompliant(idToken.getClass(), params);

        return isValid;
    }

    @Override
    public void setSingleSignOnState(final MicrosoftAccount account,
                                     final MicrosoftRefreshToken refreshToken) {
        final String methodName = "setSingleSignOnState";

        try {
            final Account accountDto = mAccountCredentialAdapter.asAccount(account);
            final com.microsoft.identity.common.internal.dto.RefreshToken rt = mAccountCredentialAdapter.asRefreshToken(refreshToken);
            final IdToken idToken = mAccountCredentialAdapter.asIdToken(account, refreshToken);

            validateCacheArtifacts(
                    accountDto,
                    null,
                    rt,
                    idToken
            );

            mAccountCredentialCache.saveAccount(accountDto);
            mAccountCredentialCache.saveCredential(idToken);
            mAccountCredentialCache.saveCredential(rt);
        } catch (ClientException e) {
            // TODO how do I know that it's safe to log this Exception?
            Logger.error(
                    TAG + ":" + methodName,
                    "",
                    new IllegalArgumentException(
                            "Cannot set SSO state. Invalid or inadequate Account and/or token provided. (See logs)",
                            e
                    )
            );
        }
    }

    @Override
    public MicrosoftRefreshToken getSingleSignOnState(final MicrosoftAccount account) {
        final MicrosoftRefreshToken result = null;
        // TODO

        return result;
    }

}
