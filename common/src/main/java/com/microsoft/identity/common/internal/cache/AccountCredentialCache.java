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

public class AccountCredentialCache
        extends OAuth2TokenCache
        implements IShareSingleSignOnState {

    // SharedPreferences used to store Accounts and Credentials
    private final SharedPreferencesFileManager mAccountCredentialSharedPreferences;

    private List<IShareSingleSignOnState> mSharedSsoCaches;
    private ICacheConfiguration mCacheConfiguration;

    // The names of the SharedPreferences file on disk.
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    public AccountCredentialCache(final Context context,
                                  final List<IShareSingleSignOnState> sharedSsoCaches,
                                  final ICacheConfiguration cacheConfiguration) {
        super(context);
        mAccountCredentialSharedPreferences =
                new SharedPreferencesFileManager(mContext, sAccountCredentialSharedPreferences);
        mSharedSsoCaches = sharedSsoCaches;
        mCacheConfiguration = cacheConfiguration;
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
        final ICredentialFactory credentialFactory = mCacheConfiguration.getCredentialFactory();
        final ICacheHelper<com.microsoft.identity.common.internal.dto.RefreshToken> rtCacheHelper = mCacheConfiguration.getRefreshTokenCacheHelper();
        final com.microsoft.identity.common.internal.dto.RefreshToken refreshToken = credentialFactory.createRefreshToken(oAuth2Strategy, request, response);
        final String rtCacheKey = rtCacheHelper.createCacheKey(refreshToken);
        final String rtCacheValue = rtCacheHelper.createCacheKey(refreshToken);
        mAccountCredentialSharedPreferences.putString(rtCacheKey, rtCacheValue);
    }

    private void saveAccessToken(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final ICredentialFactory credentialFactory = mCacheConfiguration.getCredentialFactory();
        final ICacheHelper<AccessToken> atCacheHelper = mCacheConfiguration.getAccessTokenCacheHelper();
        final AccessToken accessToken = credentialFactory.createAccessToken(oAuth2Strategy, request, response);
        final String atCacheKey = atCacheHelper.createCacheKey(accessToken);
        final String atCacheValue = atCacheHelper.createCacheKey(accessToken);
        mAccountCredentialSharedPreferences.putString(atCacheKey, atCacheValue);
    }

    private void saveAccount(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        final IAccountFactory accountFactory = mCacheConfiguration.getAccountFactory();
        final ICacheHelper<Account> accountCacheHelper = mCacheConfiguration.getAccountCacheHelper();
        final Account accountToSave = accountFactory.createAccount(oAuth2Strategy, request, response);
        final String accountCacheKey = accountCacheHelper.createCacheKey(accountToSave);
        final String accountCacheValue = accountCacheHelper.createCacheKey(accountToSave);
        mAccountCredentialSharedPreferences.putString(accountCacheKey, accountCacheValue);
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
