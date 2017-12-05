package com.microsoft.identity.common.internal.cache;

import android.content.Context;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * Cache representation for MSAL.
 */
public class MSALOAuth2TokenCache extends OAuth2TokenCache implements IShareSingleSignOnState {

    // SharedPreferences used to store tokens
    private final SharedPreferencesFileManager mAccessTokenSharedPreferences;
    private final SharedPreferencesFileManager mRefreshTokenSharedPreferences;

    // The names of the SharedPreferences files on disk
    private static final String sAccessTokenSharedPreferences = "com.microsoft.identity.client.token";
    private static final String sRefreshTokenSharedPreferences = "com.microsoft.identity.client.refreshToken";

    /**
     * Constructs a new MSALOAuth2TokenCache.
     *
     * @param context The Application consuming this library.
     */
    public MSALOAuth2TokenCache(Context context) {
        super(context);
        mAccessTokenSharedPreferences = new SharedPreferencesFileManager(mContext, sAccessTokenSharedPreferences);
        mRefreshTokenSharedPreferences = new SharedPreferencesFileManager(mContext, sRefreshTokenSharedPreferences);
    }

    @Override
    public void setSingleSignOnState(Account account, RefreshToken refreshToken) {
        // TODO implement
    }

    @Override
    public RefreshToken getSingleSignOnState(Account account) {
        // TODO implement
        return null;
    }

    @Override
    public void saveTokens(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        saveAccessToken(oAuth2Strategy, request, response);
        saveRefreshToken(oAuth2Strategy, request, response);
    }

    /**
     * Saves the accessToken to the cache.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
    private void saveAccessToken(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        // TODO delete old tokens? Does this really make sense to do here?
        final ISelfSerializingCacheItem cacheItem = new MsalAccessTokenCacheItem(oAuth2Strategy, request, response);
        final String atCacheKey = cacheItem.getCacheKey();
        final String atCacheValue = cacheItem.getCacheValue();
        mAccessTokenSharedPreferences.putString(atCacheKey, atCacheValue);
    }

    /**
     * Saves the refreshToken to the cache.
     *
     * @param oAuth2Strategy The Strategy of this IdP.
     * @param request        The {@link AuthorizationRequest} placed.
     * @param response       The {@link TokenResponse} of this request.
     */
    private void saveRefreshToken(
            OAuth2Strategy oAuth2Strategy,
            AuthorizationRequest request,
            TokenResponse response) {
        final ISelfSerializingCacheItem cacheItem = new MsalRefreshTokenCacheItem(oAuth2Strategy, request, response);
        final String rtCacheKey = cacheItem.getCacheKey();
        final String rtCacheValue = cacheItem.getCacheValue();
        if (!StringExtensions.isNullOrBlank(response.getRefreshToken())) {
            mRefreshTokenSharedPreferences.putString(rtCacheKey, rtCacheValue);
        }
    }
}
