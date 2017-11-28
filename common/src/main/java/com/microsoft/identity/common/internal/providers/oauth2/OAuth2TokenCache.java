package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;

/**
 * Class for managing the tokens saved locally on a device
 */
public abstract class OAuth2TokenCache {

    protected Context mContext;

    public OAuth2TokenCache(Context context) {
        mContext = context;
    }

    /**
     * @param oAuth2Strategy
     * @param request
     * @param response
     */
    public abstract void saveTokens(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response);
}
