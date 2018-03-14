package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.List;

public class MsalAccountCredentialCache
        extends OAuth2TokenCache
        implements IShareSingleSignOnState {

    // SharedPreferences used to store Accounts and Credentials
    private final SharedPreferencesFileManager mAccountCredentialSharedPreferences;

    private List<IShareSingleSignOnState> mSharedSsoCaches;
    private ICacheHelper<Account> mAccountCacheHelper;
    private ICacheHelper<Credential> mCredentialCacheHelper;

    // The names of the SharedPreferences file on disk.
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    public MsalAccountCredentialCache(final Context context,
                                      final List<IShareSingleSignOnState> sharedSsoCaches,
                                      final ICacheHelper<Account> accountCacheHelper,
                                      final ICacheHelper<Credential> credentialCacheHelper) {
        super(context);
        mAccountCredentialSharedPreferences =
                new SharedPreferencesFileManager(mContext, sAccountCredentialSharedPreferences);
        mSharedSsoCaches = sharedSsoCaches;
        mAccountCacheHelper = accountCacheHelper;
        mCredentialCacheHelper = credentialCacheHelper;
    }

    @Override
    public void saveTokenResponse(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        saveAccount(createAccount(oAuth2Strategy, request, response));
        saveCredential(createCredential(oAuth2Strategy, request, response));
    }

    private void saveAccount(final Account account) {
        final String accountCacheKey = mAccountCacheHelper.createCacheKey(account);
        final String accountCacheValue = mAccountCacheHelper.getCacheValue(account);
        mAccountCredentialSharedPreferences.putString(accountCacheKey, accountCacheValue);
    }

    private void saveCredential(final Credential credential) {
        final String credentialCacheKey = mCredentialCacheHelper.createCacheKey(credential);
        final String credentialCacheValue = mCredentialCacheHelper.getCacheValue(credential);
        mAccountCredentialSharedPreferences.putString(credentialCacheKey, credentialCacheValue);
    }

    private Account createAccount(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        // TODO
        return null;
    }

    private Credential createCredential(final OAuth2Strategy oAuth2Strategy,
                                        final AuthorizationRequest request,
                                        final TokenResponse response) {
        // TODO
        return null;
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
