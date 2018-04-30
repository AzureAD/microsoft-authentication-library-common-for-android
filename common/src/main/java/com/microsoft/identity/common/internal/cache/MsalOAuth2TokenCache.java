package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MsalOAuth2TokenCache
        extends OAuth2TokenCache
        implements IShareSingleSignOnState {

    private static final String TAG = MsalOAuth2TokenCache.class.getSimpleName();

    private List<IShareSingleSignOnState> mSharedSsoCaches;
    private IAccountCredentialCache mAccountCredentialCache;
    private IAccountCredentialAdapter mAccountCredentialAdapter;
    private ISsoValidator mSsoValidator;

    public MsalOAuth2TokenCache(final Context context,
                                final IAccountCredentialCache accountCredentialCache,
                                final IAccountCredentialAdapter accountCredentialAdapter,
                                final ISsoValidator ssoValidator) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        mAccountCredentialCache = accountCredentialCache;
        mSharedSsoCaches = new ArrayList<>();
        mAccountCredentialAdapter = accountCredentialAdapter;
        mSsoValidator = ssoValidator;
    }

    public MsalOAuth2TokenCache(final Context context,
                                final IAccountCredentialCache accountCredentialCache,
                                final IAccountCredentialAdapter accountCredentialAdapter,
                                final ISsoValidator ssoValidator,
                                final List<IShareSingleSignOnState> sharedSsoCaches) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        mAccountCredentialCache = accountCredentialCache;
        mSharedSsoCaches = sharedSsoCaches;
        mAccountCredentialAdapter = accountCredentialAdapter;
        mSsoValidator = ssoValidator;
    }

    @Override
    public void saveTokens(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "saveTokens";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);
        saveAccount(oAuth2Strategy, request, response);
        saveCredentials(oAuth2Strategy, request, response);
        Logger.exiting(TAG, methodName);
    }

    private void saveCredentials(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "saveCredentials";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);
        saveAccessToken(oAuth2Strategy, request, response);
        saveRefreshToken(oAuth2Strategy, request, response);
        saveIdToken(oAuth2Strategy, request, response);
        Logger.exiting(TAG, methodName);
    }

    private void saveIdToken(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "saveIdToken";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);
        final IdToken idToken = mAccountCredentialAdapter.createIdToken(oAuth2Strategy, request, response);
        mAccountCredentialCache.saveCredential(idToken);
        Logger.exiting(TAG, methodName);
    }

    private void saveRefreshToken(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "saveRefreshToken";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);
        final com.microsoft.identity.common.internal.dto.RefreshToken refreshToken = mAccountCredentialAdapter.createRefreshToken(oAuth2Strategy, request, response);
        mAccountCredentialCache.saveCredential(refreshToken);
        Logger.exiting(TAG, methodName);
    }

    private void saveAccessToken(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "saveAccessToken";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);
        final AccessToken accessToken = mAccountCredentialAdapter.createAccessToken(oAuth2Strategy, request, response);
        deleteAccessTokensWithIntersectingScopes(accessToken);
        mAccountCredentialCache.saveCredential(accessToken);
        Logger.exiting(TAG, methodName);
    }

    private void deleteAccessTokensWithIntersectingScopes(final AccessToken referenceToken) {
        final String methodName = "deleteAccessTokensWithIntersectingScopes";
        Logger.entering(TAG, methodName, referenceToken);

        final List<Credential> accessTokens = mAccountCredentialCache.getCredentials(
                referenceToken.getUniqueUserId(),
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

    private void saveAccount(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final String methodName = "saveAccount";
        Logger.entering(TAG, methodName, oAuth2Strategy, request, response);
        final Account accountToSave = mAccountCredentialAdapter.createAccount(oAuth2Strategy, request, response);
        Logger.info(TAG + ":" + methodName, "Saving Account...");
        Logger.infoPII(TAG + ":" + methodName, "Account: " + accountToSave);
        mAccountCredentialCache.saveAccount(accountToSave);
        Logger.exiting(TAG, methodName);
    }

    @Override
    public void setSingleSignOnState(final com.microsoft.identity.common.Account account,
                                     final RefreshToken refreshToken) {
        final String methodName = "setSingleSignOnState";
        Logger.entering(TAG, methodName, account, refreshToken);

        final boolean argumentsValid = validateSingleSignOnStateArguments(account, refreshToken);

        if (argumentsValid) {
            final com.microsoft.identity.common.internal.dto.RefreshToken rt = mAccountCredentialAdapter.asRefreshToken(refreshToken);
            final Account accountDto = mAccountCredentialAdapter.asAccount(account);
            mAccountCredentialCache.saveAccount(accountDto);
            mAccountCredentialCache.saveCredential(rt);
        } else {
            Logger.error(
                    TAG + ":" + methodName,
                    "",
                    new IllegalArgumentException("Cannot set SSO state. Invalid or inadequate Account and/or token provided. (See logs)")
            );
        }

        Logger.exiting(TAG, methodName);
    }

    private boolean validateSingleSignOnStateArguments(
            final com.microsoft.identity.common.Account account,
            final RefreshToken refreshToken) {
        final String methodName = "validateSingleSignOnStateArguments";
        Logger.entering(TAG, methodName, account, refreshToken);

        boolean valid = mSsoValidator.isAccountValid(account) & mSsoValidator.isRefreshTokenValid(refreshToken);

        Logger.exiting(TAG, methodName, valid);

        return valid;
    }

    @Override
    public RefreshToken getSingleSignOnState(final com.microsoft.identity.common.Account account) {
        final String methodName = "getSingleSignOnState";
        Logger.entering(TAG, methodName, account);
        final RefreshToken result = null;
        // TODO
        Logger.exiting(TAG, methodName, result);
        return result;
    }
}
