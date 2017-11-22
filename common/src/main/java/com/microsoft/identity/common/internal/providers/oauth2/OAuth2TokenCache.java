package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.Account;
/**
 * Class for managing the tokens saved locally on a device
 */
public abstract class OAuth2TokenCache {



    /**
     *
     * @param oAuth2Strategy
     * @param request
     * @param response
     */
    public abstract void saveTokens(OAuth2Strategy oAuth2Strategy, TokenRequest request, TokenResponse response);
}
