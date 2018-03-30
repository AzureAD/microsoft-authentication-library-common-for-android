package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MsalOAuth2TokenCache
        extends OAuth2TokenCache
        implements IShareSingleSignOnState {

    private List<IShareSingleSignOnState> mSharedSsoCaches;
    private IAccountCredentialCache mAccountCredentialCache;
    private IAccountCredentialAdapter mAccountCredentialAdapter;

    public MsalOAuth2TokenCache(final Context context,
                                final IAccountCredentialCache accountCredentialCache,
                                final IAccountCredentialAdapter accountCredentialAdapter,
                                final List<IShareSingleSignOnState> sharedSsoCaches) {
        super(context);
        mAccountCredentialCache = accountCredentialCache;
        mSharedSsoCaches = sharedSsoCaches;
        mAccountCredentialAdapter = accountCredentialAdapter;
    }

    @Override
    public void saveTokens(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        saveAccount(oAuth2Strategy, request, response);
        saveCredentials(oAuth2Strategy, request, response);
    }

    private void saveCredentials(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        saveAccessToken(oAuth2Strategy, request, response);
        saveRefreshToken(oAuth2Strategy, request, response);
    }

    private void saveRefreshToken(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final com.microsoft.identity.common.internal.dto.RefreshToken refreshToken = mAccountCredentialAdapter.createRefreshToken(oAuth2Strategy, request, response);
        mAccountCredentialCache.saveCredential(refreshToken);
    }

    private void saveAccessToken(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final AccessToken accessToken = mAccountCredentialAdapter.createAccessToken(oAuth2Strategy, request, response);
        deleteAccessTokensWithIntersectingScopes(accessToken);
        mAccountCredentialCache.saveCredential(accessToken);
    }

    private void deleteAccessTokensWithIntersectingScopes(final AccessToken referenceToken) {
        final List<Credential> accessTokens = mAccountCredentialCache.getCredentials(
                referenceToken.getUniqueId(),
                referenceToken.getEnvironment(),
                CredentialType.AccessToken,
                referenceToken.getClientId(),
                referenceToken.getRealm(),
                null // Wildcard - delete anything that matches...
        );

        for (final Credential accessToken : accessTokens) {
            if (scopesIntersect(referenceToken, (AccessToken) accessToken)) {
                mAccountCredentialCache.removeCredential(accessToken);
            }
        }
    }

    private boolean scopesIntersect(final AccessToken token1, final AccessToken token2) {
        final Set<String> token1Scopes = scopesAsSet(token1);
        final Set<String> token2Scopes = scopesAsSet(token2);

        for (final String scope : token2Scopes) {
            if (token1Scopes.contains(scope)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> scopesAsSet(final AccessToken token) {
        final Set<String> scopeSet = new HashSet<>();
        final String scopeString = token.getTarget();
        final String[] scopeArray = scopeString.split("\\s+");
        scopeSet.addAll(Arrays.asList(scopeArray));
        return scopeSet;
    }

    private void saveAccount(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final Account accountToSave = mAccountCredentialAdapter.createAccount(oAuth2Strategy, request, response);
        mAccountCredentialCache.saveAccount(accountToSave);
    }

    @Override
    public void setSingleSignOnState(final com.microsoft.identity.common.Account account,
                                     final RefreshToken refreshToken) {
        // TODO
    }

    @Override
    public RefreshToken getSingleSignOnState(final com.microsoft.identity.common.Account account) {
        // TODO
        return null;
    }
}
