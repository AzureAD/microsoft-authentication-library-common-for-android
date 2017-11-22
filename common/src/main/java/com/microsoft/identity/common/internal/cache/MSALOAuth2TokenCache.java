package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;



public class MSALOAuth2TokenCache extends OAuth2TokenCache implements IShareSingleSignOnState {

    public MSALOAuth2TokenCache(Context context) {
        super(context);
    }

    @Override
    public void setSingleSignOnState(Account account, String refreshToken) {

    }

    @Override
    public String getSingleSignOnState(Account account) {
        return null;
    }

    @Override
    public void saveTokens(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) {

    }
}
