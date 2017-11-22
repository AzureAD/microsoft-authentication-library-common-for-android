package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.media.session.MediaSession;
import android.util.Log;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.net.MalformedURLException;

public class ADALOAuth2TokenCache extends OAuth2TokenCache implements IShareSingleSignOnState {

    final static String SHARED_PREFERENCES_FILENAME = "com.microsoft.aad.adal.cache";

    public ADALOAuth2TokenCache(Context context, SharedPreferencesFileManager mSharedPreferencesFileManager) {
        super(context);
        InitializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
    }

    @Override
    public void saveTokens(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) {

        AzureActiveDirectoryAccount aadAccount;
        AzureActiveDirectoryTokenResponse aadTokenResponse;
        Account account = oAuth2Strategy.createAccount(response);

        if(account instanceof  AzureActiveDirectoryAccount){
            aadAccount = (AzureActiveDirectoryAccount)account;
        }else{
            throw new IllegalArgumentException("ADALOAuth2TokenCache expects an AAD Account Object");
        }

        if(response instanceof AzureActiveDirectoryTokenResponse ){
            aadTokenResponse = (AzureActiveDirectoryTokenResponse)response;
        }else{
            throw new IllegalArgumentException("ADALOAuthTokenCache expects an AAD Token Response Object");
        }

        if (aadAccount != null) {
            // update cache entry with displayableId
            if (!StringExtensions.isNullOrBlank(aadAccount.getDisplayableId())) {
                setItemToCacheForUser(request.getScope(), request.getClientId(), aadTokenResponse, aadAccount.getDisplayableId());
            }

            // TODO: I changes the behavior of this method... to return the new user id based on utid, uid... can't do that....
            if (!StringExtensions.isNullOrBlank(aadAccount.getUserIdentifier())) {
                setItemToCacheForUser(request.getScope(), request.getClientId(), aadTokenResponse, aadAccount.getUserIdentifier());
            }
        }

        // TODO: Since this is AAD and not ADFS... I think we can remove this....
        //setItemToCacheForUser(resource, clientId, result, null);
    }

    private void setItemToCacheForUser(final String resource, final String clientId, final AzureActiveDirectoryTokenResponse result, final String userId)  {


        // new tokens will only be saved into preferred cache location
        mTokenCacheStore.setItem(CacheKey.createCacheKeyForRTEntry(getAuthorityUrlWithPreferredCache(), resource, clientId, userId),
                TokenCacheItem.createRegularTokenCacheItem(getAuthorityUrlWithPreferredCache(), resource, clientId, result));


        if (result.getIsMultiResourceRefreshToken()) {
            Log.v(TAG, "Save Multi Resource Refresh token to cache");
            mTokenCacheStore.setItem(CacheKey.createCacheKeyForMRRT(getAuthorityUrlWithPreferredCache(), clientId, userId),
                    TokenCacheItem.createMRRTTokenCacheItem(getAuthorityUrlWithPreferredCache(), clientId, result));
        }

        // Store separate entries for FRT.
        if (!StringExtensions.isNullOrBlank(result.getFamilyClientId()) && !StringExtensions.isNullOrBlank(userId)) {
            Log.v(TAG, "Save Family Refresh token into cache");
            final TokenCacheItem familyTokenCacheItem = TokenCacheItem.createFRRTTokenCacheItem(getAuthorityUrlWithPreferredCache(), result);
            mTokenCacheStore.setItem(CacheKey.createCacheKeyForFRT(getAuthorityUrlWithPreferredCache(), result.getFamilyClientId(), userId), familyTokenCacheItem);
        }

    }

    @Override
    public void setSingleSignOnState(Account account, String refreshToken) {

    }

    @Override
    public String getSingleSignOnState(Account account) {
        return null;
    }
}
