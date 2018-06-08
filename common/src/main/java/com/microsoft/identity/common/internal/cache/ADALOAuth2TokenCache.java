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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.cache.CacheKey;
import com.microsoft.identity.common.adal.internal.cache.DateTimeAdapter;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
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

/**
 * Class responsible for saving oAuth2 Tokens for use in future requests.  Ideally this class would
 * work with any IDP; however ADAL only currently supports ADFS and AAD hence this class reflects that
 */
public class ADALOAuth2TokenCache
        extends OAuth2TokenCache<AzureActiveDirectoryOAuth2Strategy, AzureActiveDirectoryAuthorizationRequest, AzureActiveDirectoryTokenResponse>
        implements IShareSingleSignOnState {
    private ISharedPreferencesFileManager mISharedPreferencesFileManager;

    private static final String TAG = ADALOAuth2TokenCache.class.getSimpleName();
    private static final String SHARED_PREFERENCES_FILENAME = "com.microsoft.aad.adal.cache";
    private static final Object LOCK = new Object();

    @SuppressLint("StaticFieldLeak")
    private static StorageHelper sHelper;

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    private List<IShareSingleSignOnState> mSharedSSOCaches;

    /**
     * Constructor of ADALOAuth2TokenCache.
     *
     * @param context Context
     */
    public ADALOAuth2TokenCache(final Context context) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = new ArrayList<>();
    }

    /**
     * Constructor of ADALOAuth2TokenCache.
     *
     * @param context         Context
     * @param sharedSSOCaches List<IShareSingleSignOnState>
     */
    public ADALOAuth2TokenCache(final Context context,
                                final List<IShareSingleSignOnState> sharedSSOCaches) {
        super(context);
        Logger.verbose(TAG, "Init: " + TAG);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = sharedSSOCaches;
    }

    protected void initializeSharedPreferencesFileManager(final String fileName) {
        mISharedPreferencesFileManager = new SharedPreferencesFileManager(getContext(), fileName);
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
        final String methodName = "saveTokens";
        Logger.info(TAG + ":" + methodName, "Saving Tokens...");

        final Account account = strategy.createAccount(response);
        final String issuerCacheIdentifier = strategy.getIssuerCacheIdentifier(request);
        final RefreshToken refreshToken = strategy.getRefreshTokenFromResponse(response);

        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem(strategy, request, response);

        //There is more than one valid user identifier for some accounts... AAD Accounts as of this writing have 3
        Logger.info(TAG + ":" + methodName, "Setting items to cache for user...");
        for (final String cacheIdentifier : account.getCacheIdentifiers()) {
            //Azure AD Uses Resource and Not Scope... but we didn't override... heads up
            final String scope = request.getScope();
            final String clientId = request.getClientId();

            Logger.infoPII(TAG + ":" + methodName, "issuerCacheIdentifier: [" + issuerCacheIdentifier + "]");
            Logger.infoPII(TAG + ":" + methodName, "scope: [" + scope + "]");
            Logger.infoPII(TAG + ":" + methodName, "clientId: [" + clientId + "]");
            Logger.infoPII(TAG + ":" + methodName, "cacheIdentifier: [" + cacheIdentifier + "]");

            setItemToCacheForUser(issuerCacheIdentifier, scope, clientId, cacheItem, cacheIdentifier);
        }

        //For legacy reasons creating a cache entry where the userid is null
        //ADAL supported a single user mode where it was not necessary for the developer to provide the user id
        //on calls to acquireTokenSilentAsync
        setItemToCacheForUser(issuerCacheIdentifier, request.getScope(), request.getClientId(), cacheItem, null);

        // TODO At some point, the type-safety of this call needs to get beefed-up
        Logger.info(TAG + ":" + methodName, "Syncing SSO state to caches...");
        for (final IShareSingleSignOnState sharedSsoCache : mSharedSSOCaches) {
            sharedSsoCache.setSingleSignOnState(account, refreshToken);
        }
    }


    private void setItemToCacheForUser(final String issuer,
                                       final String resource,
                                       final String clientId,
                                       final ADALTokenCacheItem cacheItem,
                                       final String userId) {
        final String methodName = "setItemToCacheForUser";

        setItem(CacheKey.createCacheKeyForRTEntry(issuer, resource, clientId, userId), cacheItem);

        if (cacheItem.getIsMultiResourceRefreshToken()) {
            Logger.info(TAG + ":" + methodName, "CacheItem is an MRRT.");
            setItem(CacheKey.createCacheKeyForMRRT(issuer, clientId, userId), cacheItem);
        }

        if (!StringExtensions.isNullOrBlank(cacheItem.getFamilyClientId())) {
            Logger.info(TAG + ":" + methodName, "CacheItem is an FRT.");
            setItem(CacheKey.createCacheKeyForFRT(issuer, cacheItem.getFamilyClientId(), userId), cacheItem);
        }
    }

    private void setItem(final String key, final ADALTokenCacheItem cacheItem) {
        final String methodName = "setItem";

        String json = mGson.toJson(cacheItem);
        String encrypted = encrypt(json);

        if (encrypted != null) {
            mISharedPreferencesFileManager.putString(key, encrypted);
        } else {
            Logger.error(TAG + ":" + methodName, "Encrypted output was null.", null);
        }
    }


    /**
     * Method that allows to mock StorageHelper class and use custom encryption in UTs.
     */
    protected StorageHelper getStorageHelper() {
        final String methodName = "getStorageHelper";

        synchronized (LOCK) {
            if (sHelper == null) {
                Logger.verbose(TAG + ":" + methodName, "Initializing StorageHelper");
                sHelper = new StorageHelper(getContext());
                Logger.verbose(TAG + ":" + methodName, "Finished initializing StorageHelper");
            }
        }


        return sHelper;
    }

    private String encrypt(final String value) {
        final String methodName = "encrypt";

        String encryptedResult = null;
        try {
            encryptedResult = getStorageHelper().encrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.error(TAG + ":" + methodName, "Failed to encrypt input value.", null);
        }

        return encryptedResult;
    }

    private String decrypt(final String key, final String value) { //NOPMD Suppressing PMD warning for unused method
        if (StringExtensions.isNullOrBlank(key)) {
            throw new IllegalArgumentException("encryption key is null or blank");
        }

        String decryptedResult;

        try {
            decryptedResult = getStorageHelper().decrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.errorPII(TAG, ErrorStrings.DECRYPTION_ERROR, e);
            decryptedResult = null;

            //TODO: Implement remove item in this case... not sure I actually want to do this
            //removeItem(key);
        }

        return decryptedResult;
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
        // Unimplemented
        final RefreshToken refreshToken = null;
        return refreshToken;
    }
}
