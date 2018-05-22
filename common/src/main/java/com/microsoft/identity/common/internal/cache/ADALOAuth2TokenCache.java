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
        final String methodName = "initializeSharedPreferencesFileManager";
        Logger.entering(TAG, methodName, fileName);
        mISharedPreferencesFileManager = new SharedPreferencesFileManager(getContext(), fileName);
        Logger.exiting(TAG, methodName);
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
        Logger.entering(TAG, methodName, strategy, request, response);
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
            Logger.infoPII(TAG + ":" + methodName, "cacheItem: [" + cacheItem + "]");
            Logger.infoPII(TAG + ":" + methodName, "cacheIdentifier: [" + cacheIdentifier + "]");

            setItemToCacheForUser(issuerCacheIdentifier, scope, clientId, cacheItem, cacheIdentifier);
        }

        // TODO At some point, the type-safety of this call needs to get beefed-up
        Logger.info(TAG + ":" + methodName, "Syncing SSO state to caches...");
        for (final IShareSingleSignOnState sharedSsoCache : mSharedSSOCaches) {
            sharedSsoCache.setSingleSignOnState(account, refreshToken);
        }

        // TODO: I'd like to know exactly why this is here before I put this back in.... i'm assuming for ADFS v3.
        //setItemToCacheForUser(resource, clientId, result, null);

        Logger.exiting(TAG, methodName);
    }


    private void setItemToCacheForUser(final String issuer,
                                       final String resource,
                                       final String clientId,
                                       final ADALTokenCacheItem cacheItem,
                                       final String userId) {
        final String methodName = "setItemToCacheForUser";
        Logger.entering(TAG, methodName, issuer, resource, clientId, cacheItem, userId);

        setItem(CacheKey.createCacheKeyForRTEntry(issuer, resource, clientId, userId), cacheItem);

        if (cacheItem.getIsMultiResourceRefreshToken()) {
            Logger.info(TAG + ":" + methodName, "CacheItem is an MRRT.");
            setItem(CacheKey.createCacheKeyForMRRT(issuer, clientId, userId), cacheItem);
        }

        if (!StringExtensions.isNullOrBlank(cacheItem.getFamilyClientId())) {
            Logger.info(TAG + ":" + methodName, "CacheItem is an FRT.");
            setItem(CacheKey.createCacheKeyForFRT(issuer, cacheItem.getFamilyClientId(), userId), cacheItem);
        }

        Logger.exiting(TAG, methodName);
    }

    private void setItem(final String key, final ADALTokenCacheItem cacheItem) {
        final String methodName = "setItem";
        Logger.entering(TAG, methodName, key, cacheItem);

        String json = mGson.toJson(cacheItem);
        String encrypted = encrypt(json);

        Logger.verbosePII(TAG + ":" + methodName, "Derived JSON: " + json);

        if (encrypted != null) {
            mISharedPreferencesFileManager.putString(key, encrypted);
        } else {
            Logger.error(TAG + ":" + methodName, "Encrypted output was null.", null);
        }

        Logger.exiting(TAG, methodName);
    }


    /**
     * Method that allows to mock StorageHelper class and use custom encryption in UTs.
     */
    protected StorageHelper getStorageHelper() {
        final String methodName = "getStorageHelper";
        Logger.entering(TAG, methodName);

        synchronized (LOCK) {
            if (sHelper == null) {
                Logger.verbose(TAG + ":" + methodName, "Initializing StorageHelper");
                sHelper = new StorageHelper(getContext());
                Logger.verbose(TAG + ":" + methodName, "Finished initializing StorageHelper");
            }
        }

        Logger.exiting(TAG, methodName, sHelper);

        return sHelper;
    }

    private String encrypt(final String value) {
        final String methodName = "encrypt";
        Logger.entering(TAG, methodName, value);

        String encryptedResult;

        try {
            encryptedResult = getStorageHelper().encrypt(value);
        } catch (GeneralSecurityException | IOException e) {
            Logger.error(TAG + ":" + methodName, "Failed to encrypt input value.", null);
            Logger.errorPII(TAG + ":" + methodName, ErrorStrings.ENCRYPTION_ERROR, e);
            encryptedResult = null;
        }

        Logger.exiting(TAG, methodName, encryptedResult);

        return encryptedResult;
    }

    private String decrypt(final String key, final String value) {
        final String methodName = "decrypt";
        Logger.entering(TAG, methodName, key, value);

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

        Logger.exiting(TAG, methodName, decryptedResult);

        return decryptedResult;
    }

    private void validateSecretKeySetting() {
        final String methodName = "validateSecretKeySetting";
        Logger.entering(TAG, methodName);

        final byte[] secretKeyData = AuthenticationSettings.INSTANCE.getSecretKeyData();

        if (secretKeyData == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            throw new IllegalArgumentException("Secret key must be provided for API < 18. "
                    + "Use AuthenticationSettings.INSTANCE.setSecretKey()");
        }

        Logger.exiting(TAG, methodName);
    }

    @Override
    public void setSingleSignOnState(final Account account, final RefreshToken refreshToken) {
        final String methodName = "setSingleSignOnState";
        Logger.entering(TAG, methodName, account, refreshToken);
        // Unimplemented

        Logger.exiting(TAG, methodName);
    }

    @Override
    public RefreshToken getSingleSignOnState(final Account account) {
        final String methodName = "getSingleSignOnState";
        Logger.entering(TAG, methodName, account);

        // Unimplemented
        final RefreshToken refreshToken = null;

        Logger.exiting(TAG, methodName, refreshToken);

        return refreshToken;
    }
}
