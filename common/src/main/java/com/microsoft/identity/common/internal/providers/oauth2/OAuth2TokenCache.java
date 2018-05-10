package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;

import com.microsoft.identity.common.exception.ClientException;

/**
 * Class for managing the tokens saved locally on a device
 */
public abstract class OAuth2TokenCache
        <T extends OAuth2Strategy, U extends AuthorizationRequest, V extends TokenResponse> {

    protected Context mContext;

    /**
     * Constructs a new OAuth2TokenCache.
     *
     * @param context The Application Context of the consuming app.
     */
    public OAuth2TokenCache(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Saves the credentials and tokens returned by the service to the cache.
     *
     * @param oAuth2Strategy The strategy used to create the token request.
     * @param request        The request used to acquire tokens and credentials.
     * @param response       The response received from the IdP/STS.
     * @throws ClientException If tokens cannot be successfully saved.
     */
    public abstract void saveTokens(final T oAuth2Strategy,
                                    final U request,
                                    final V response) throws ClientException;
}
