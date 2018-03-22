package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.List;

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
        mAccountCredentialCache.saveCredential(accessToken);
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
