package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.media.session.MediaSession;
import android.util.Log;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * Class responsible for saving oAuth2 Tokens for use in future requests.  Ideally this class would
 * work with any IDP; however ADAL only currently supports ADFS and AAD hence this class reflects that
 */
public class ADALOAuth2TokenCache extends OAuth2TokenCache implements IShareSingleSignOnState {

    SharedPreferencesFileManager mSharedPreferencesFileManager;
    final static String SHARED_PREFERENCES_FILENAME = "com.microsoft.aad.adal.cache";

    public ADALOAuth2TokenCache(Context context, SharedPreferencesFileManager mSharedPreferencesFileManager) {
        super(context);
        InitializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
    }

    protected void InitializeSharedPreferencesFileManager(String fileName){
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(super.mContext, fileName);
    }

    /**
     * Method responsible for saving tokens contained in the TokenResponse to storage.
     * @param oAuth2Strategy
     * @param request
     * @param response
     */
    @Override
    public void saveTokens(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) {

        Account account = oAuth2Strategy.createAccount(response);
        String issuerCacheIdentifier = oAuth2Strategy.getIssuerCacheIdentifier(request);
        AccessToken accessToken = oAuth2Strategy.getAccessTokenFromResponse(response);
        RefreshToken refreshToken = oAuth2Strategy.getRefreshTokenFromResponse(response);

        //There is more than one valid user identifier for some accounts... AAD Accounts as of this writing have 3
        ListIterator<String> cacheIds = account.getCacheIdentifiers().listIterator();

        while(cacheIds.hasNext()){
            //Azure AD Uses Resource and Not Scope... but we didn't override... heads up
            setItemToCacheForUser(issuerCacheIdentifier, request.getScope(), request.getClientId(), accessToken, refreshToken , cacheIds.next());
        }

        // TODO: I'd like to know exactly why this is here before I put this back in.... i'm assuming for ADFS v3.
        //setItemToCacheForUser(resource, clientId, result, null);
    }



    private void setItemToCacheForUser(final String issuer, final String resource, final String clientId, final AccessToken accessToken, final RefreshToken refreshToken, final String userId)  {



/*
        // new tokens will only be saved into preferred cache location
        mTokenCacheStore.setItem(CacheKey.createCacheKeyForRTEntry(issuer, resource, clientId, userId),
                TokenCacheItem.createRegularTokenCacheItem(issuer, resource, clientId, result));


        if (result.getIsMultiResourceRefreshToken()) {
            Log.v(TAG, "Save Multi Resource Refresh token to cache");
            mTokenCacheStore.setItem(CacheKey.createCacheKeyForMRRT(issuer, clientId, userId),
                    TokenCacheItem.createMRRTTokenCacheItem(issuer, clientId, result));
        }

        // Store separate entries for FRT.
        if (!StringExtensions.isNullOrBlank(result.getFamilyClientId()) && !StringExtensions.isNullOrBlank(userId)) {
            Log.v(TAG, "Save Family Refresh token into cache");
            final TokenCacheItem familyTokenCacheItem = TokenCacheItem.createFRRTTokenCacheItem(issuer, result);
            mTokenCacheStore.setItem(CacheKey.createCacheKeyForFRT(issuer, result.getFamilyClientId(), userId), familyTokenCacheItem);
        }
        */

    }



    @Override
    public void setSingleSignOnState(Account account, String refreshToken) {

    }

    @Override
    public String getSingleSignOnState(Account account) {
        return null;
    }
}
