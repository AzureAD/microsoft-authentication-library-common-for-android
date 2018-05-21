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
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.microsoft.identity.common.exception.ErrorStrings.ACCOUNT_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.exception.ErrorStrings.CREDENTIAL_IS_SCHEMA_NONCOMPLIANT;

public class MsalOAuth2TokenCache
        extends OAuth2TokenCache<MicrosoftStsOAuth2Strategy, MicrosoftStsAuthorizationRequest, MicrosoftStsTokenResponse>
        implements IShareSingleSignOnState<MicrosoftAccount, RefreshToken> {

    private static final String TAG = MsalOAuth2TokenCache.class.getSimpleName();

    private List<IShareSingleSignOnState> mSharedSsoCaches;
    private IAccountCredentialCache mAccountCredentialCache;

    private IAccountCredentialAdapter<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            RefreshToken> mAccountCredentialAdapter;

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
                                        RefreshToken> accountCredentialAdapter) {
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
                                        RefreshToken> accountCredentialAdapter,
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
        final String methodName = "saveTokensV2";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);

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

        Logger.exiting(TAG, methodName);
    }

    private void saveAccounts(final Account... accounts) {
        final String methodName = "saveAccounts";
        Logger.entering(TAG, methodName, accounts);

        for (final Account account : accounts) {
            mAccountCredentialCache.saveAccount(account);
        }

        Logger.exiting(TAG, methodName);
    }

    private void saveCredentials(final Credential... credentials) {
        final String methodName = "saveCredentials";
        Logger.entering(TAG, methodName, credentials);

        for (final Credential credential : credentials) {

            if (credential instanceof AccessToken) {
                deleteAccessTokensWithIntersectingScopes((AccessToken) credential);
            }

            mAccountCredentialCache.saveCredential(credential);
        }

        Logger.exiting(TAG, methodName);
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
        final String methodName = "validateCacheArtifacts";
        Logger.entering(TAG, methodName, accountToSave, accessTokenToSave, refreshTokenToSave, idTokenToSave);

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

        Logger.exiting(TAG, methodName);
    }

    private void deleteAccessTokensWithIntersectingScopes(final AccessToken referenceToken) {
        final String methodName = "deleteAccessTokensWithIntersectingScopes";
        Logger.entering(TAG, methodName, referenceToken);

        final List<Credential> accessTokens = mAccountCredentialCache.getCredentials(
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

        Logger.exiting(TAG, methodName);
    }

    private boolean scopesIntersect(final AccessToken token1, final AccessToken token2) {
        final String methodName = "scopesIntersect";
        Logger.entering(TAG, methodName, token1, token2);
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

        Logger.exiting(TAG, methodName, result);

        return result;
    }

    private Set<String> scopesAsSet(final AccessToken token) {
        final String methodName = "scopesAsSet";
        Logger.entering(TAG, methodName, token);

        final Set<String> scopeSet = new HashSet<>();
        final String scopeString = token.getTarget();

        if (!StringExtensions.isNullOrBlank(scopeString)) {
            final String[] scopeArray = scopeString.split("\\s+");
            scopeSet.addAll(Arrays.asList(scopeArray));
        }

        Logger.exiting(TAG, methodName, scopeSet);

        return scopeSet;
    }

    private static boolean isSchemaCompliant(final Class<?> clazz, final String[][] params) {
        final String methodName = "isSchemaCompliant";
        Logger.entering(TAG, methodName, clazz, params);

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

        Logger.exiting(TAG, methodName, isCompliant);

        return isCompliant;
    }

    private static boolean isAccountSchemaCompliant(@NonNull final Account account) {
        final String methodName = "isAccountSchemaCompliant";
        Logger.entering(TAG, methodName, account);

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

        Logger.exiting(TAG, methodName, isCompliant);

        return isCompliant;
    }

    private static boolean isAccessTokenSchemaCompliant(@NonNull final AccessToken accessToken) {
        final String methodName = "isAccessTokenSchemaCompliant";
        Logger.entering(TAG, methodName, accessToken);

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

        Logger.exiting(TAG, methodName, isValid);

        return isValid;
    }

    private static boolean isRefreshTokenSchemaCompliant(
            @NonNull final com.microsoft.identity.common.internal.dto.RefreshToken refreshToken) {
        final String methodName = "isRefreshTokenSchemaCompliant";
        Logger.entering(TAG, methodName, refreshToken);

        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, refreshToken.getCredentialType()},
                {Credential.SerializedNames.ENVIRONMENT, refreshToken.getEnvironment()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, refreshToken.getHomeAccountId()},
                {Credential.SerializedNames.CLIENT_ID, refreshToken.getClientId()},
                {Credential.SerializedNames.SECRET, refreshToken.getSecret()},
        };

        boolean isValid = isSchemaCompliant(refreshToken.getClass(), params);

        Logger.exiting(TAG, methodName, isValid);

        return isValid;
    }

    private static boolean isIdTokenSchemaCompliant(@NonNull final IdToken idToken) {
        final String methodName = "isIdTokenSchemaCompliant";
        Logger.entering(TAG, methodName, idToken);

        final String[][] params = new String[][]{
                {Credential.SerializedNames.HOME_ACCOUNT_ID, idToken.getHomeAccountId()},
                {Credential.SerializedNames.ENVIRONMENT, idToken.getEnvironment()},
                {IdToken.SerializedNames.REALM, idToken.getRealm()},
                {Credential.SerializedNames.CREDENTIAL_TYPE, idToken.getCredentialType()},
                {Credential.SerializedNames.CLIENT_ID, idToken.getClientId()},
                {Credential.SerializedNames.SECRET, idToken.getSecret()},
        };

        boolean isValid = isSchemaCompliant(idToken.getClass(), params);

        Logger.exiting(TAG, methodName, isValid);

        return isValid;
    }

    @Override
    public void setSingleSignOnState(final MicrosoftAccount account,
                                     final RefreshToken refreshToken) {
        final String methodName = "setSingleSignOnState";
        Logger.entering(TAG, methodName, account, refreshToken);

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
            Logger.error(
                    TAG + ":" + methodName,
                    "",
                    new IllegalArgumentException(
                            "Cannot set SSO state. Invalid or inadequate Account and/or token provided. (See logs)",
                            e
                    )
            );
        }

        Logger.exiting(TAG, methodName);
    }

    @Override
    public RefreshToken getSingleSignOnState(final MicrosoftAccount account) {
        final String methodName = "getSingleSignOnState";
        Logger.entering(TAG, methodName, account);
        final RefreshToken result = null;
        // TODO
        Logger.exiting(TAG, methodName, result);
        return result;
    }

}
