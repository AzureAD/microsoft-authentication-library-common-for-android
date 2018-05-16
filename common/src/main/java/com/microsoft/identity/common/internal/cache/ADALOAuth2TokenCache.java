// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.cache;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.cache.CacheKey;
import com.microsoft.identity.common.adal.internal.cache.DateTimeAdapter;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * Class responsible for saving oAuth2 Tokens for use in future requests.  Ideally this class would
 * work with any IDP; however ADAL only currently supports ADFS and AAD hence this class reflects that
 */
public class ADALOAuth2TokenCache
        extends OAuth2TokenCache<AzureActiveDirectoryOAuth2Strategy, AzureActiveDirectoryAuthorizationRequest, AzureActiveDirectoryTokenResponse>
        implements IShareSingleSignOnState {

    private static final String TAG = ADALOAuth2TokenCache.class.getSimpleName();
    private static final String SHARED_PREFERENCES_FILENAME = "com.microsoft.aad.adal.cache";
    private static final Object LOCK = new Object();

    @SuppressLint("StaticFieldLeak")
    private static StorageHelper sHelper;

    private ISharedPreferencesFileManager mISharedPreferencesFileManager;
    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    private List<IShareSingleSignOnState> mSharedSSOCaches;

    public ADALOAuth2TokenCache(final Context context) {
        super(context);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = new ArrayList<>();
    }

    public ADALOAuth2TokenCache(final Context context,
                                final List<IShareSingleSignOnState> sharedSSOCaches) {
        super(context);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = sharedSSOCaches;
    }

    protected void initializeSharedPreferencesFileManager(final String fileName) {
        mISharedPreferencesFileManager = new SharedPreferencesFileManager(super.mContext, fileName);
    }

    /**
     * Method responsible for saving tokens contained in the TokenResponse to storage.
     *
     * @param strategy
     * @param request
     * @param response
     */
    @Override
    public void saveTokens(
            final AzureActiveDirectoryOAuth2Strategy strategy,
            final AzureActiveDirectoryAuthorizationRequest request,
            final AzureActiveDirectoryTokenResponse response) {

        Account account = strategy.createAccount(response);
        String issuerCacheIdentifier = strategy.getIssuerCacheIdentifier(request);
        RefreshToken refreshToken = strategy.getRefreshTokenFromResponse(response);

        ADALTokenCacheItem cacheItem = new ADALTokenCacheItem(strategy, request, response);

        //There is more than one valid user identifier for some accounts... AAD Accounts as of this writing have 3
        ListIterator<String> accountCacheIds = account.getCacheIdentifiers().listIterator();

        while (accountCacheIds.hasNext()) {
            //Azure AD Uses Resource and Not Scope... but we didn't override... heads up
            setItemToCacheForUser(issuerCacheIdentifier, request.getScope(), request.getClientId(), cacheItem, accountCacheIds.next());
        }

        ListIterator<IShareSingleSignOnState> otherCaches = mSharedSSOCaches.listIterator();

        // TODO At some point, the type-safety of this call needs to get beefed-up
        while (otherCaches.hasNext()) {
            otherCaches.next().setSingleSignOnState(account, refreshToken);
        }

        // TODO: I'd like to know exactly why this is here before I put this back in.... i'm assuming for ADFS v3.
        //setItemToCacheForUser(resource, clientId, result, null);
    }


    private void setItemToCacheForUser(final String issuer,
                                       final String resource,
                                       final String clientId,
                                       final ADALTokenCacheItem cacheItem,
                                       final String userId) {
        setItem(CacheKey.createCacheKeyForRTEntry(issuer, resource, clientId, userId), cacheItem);

        if (cacheItem.getIsMultiResourceRefreshToken()) {
            setItem(CacheKey.createCacheKeyForMRRT(issuer, clientId, userId), cacheItem);
        }

        if (!StringExtensions.isNullOrBlank(cacheItem.getFamilyClientId())) {
            setItem(CacheKey.createCacheKeyForFRT(issuer, cacheItem.getFamilyClientId(), userId), cacheItem);
        }
    }

    private void setItem(final String key, final ADALTokenCacheItem cacheItem) {
        String json = mGson.toJson(cacheItem);
        String encrypted = encrypt(json);

        if (encrypted != null) {
            mISharedPreferencesFileManager.putString(key, encrypted);
        } else {
            Log.e(TAG, "Encrypted output is null");
        }
    }


    /**
     * Method that allows to mock StorageHelper class and use custom encryption in UTs.
     */
    protected StorageHelper getStorageHelper() {
        synchronized (LOCK) {
            if (sHelper == null) {
                Log.v(TAG, "Started to initialize storage helper");
                sHelper = new StorageHelper(mContext);
                Log.v(TAG, "Finished to initialize storage helper");
            }
        }

        return sHelper;
    }

    private String encrypt(String value) {
        try {
            return getStorageHelper().encrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, ErrorStrings.ENCRYPTION_ERROR, e);
        }

        return null;
    }

    private String decrypt(final String key, final String value) {
        if (StringExtensions.isNullOrBlank(key)) {
            throw new IllegalArgumentException("encryption key is null or blank");
        }

        try {
            return getStorageHelper().decrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, ErrorStrings.DECRYPTION_ERROR, e);
            //TODO: Implement remove item in this case... not sure I actually want to do this
            //removeItem(key);
        }

        return null;
    }

    private void validateSecretKeySetting() {
        final byte[] secretKeyData = AuthenticationSettings.INSTANCE.getSecretKeyData();

        if (secretKeyData == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            throw new IllegalArgumentException("Secret key must be provided for API < 18. "
                    + "Use AuthenticationSettings.INSTANCE.setSecretKey()");
        }
    }

    @Override
    public void setSingleSignOnState(final Account account, final RefreshToken refreshToken) {
        // Unimplemented
    }

    @Override
    public RefreshToken getSingleSignOnState(final Account account) {
        return null;
    }
}
