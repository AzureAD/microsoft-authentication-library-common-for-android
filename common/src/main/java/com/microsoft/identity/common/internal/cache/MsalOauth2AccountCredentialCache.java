package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public class MsalOAuth2AccountCredentialCache
        extends OAuth2TokenCache
        implements IShareSingleSignOnState {

    // SharedPreferences used to store Accounts and Credentials
    private final SharedPreferencesFileManager mAccountCredentialSharedPreferences;

    // The names of the SharedPreferences file on disk.
    private static final String sAccountCredentialSharedPreferences =
            "com.microsoft.identity.client.account_credential_cache";

    public MsalOAuth2AccountCredentialCache(Context context) {
        super(context);
        mAccountCredentialSharedPreferences =
                new SharedPreferencesFileManager(mContext, sAccountCredentialSharedPreferences);
    }

    @Override
    public void saveTokens(
            final OAuth2Strategy oAuth2Strategy,
            final AuthorizationRequest request,
            final TokenResponse response) {
        // TODO figure out how to save both an Account and Credential
    }

    @Override
    public void setSingleSignOnState(final Account account, final RefreshToken refreshToken) {

    }

    @Override
    public RefreshToken getSingleSignOnState(final Account account) {
        return null;
    }
}
