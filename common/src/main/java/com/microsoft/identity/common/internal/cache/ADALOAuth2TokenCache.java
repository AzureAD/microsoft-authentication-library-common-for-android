package com.microsoft.identity.common.internal.cache;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.adal.error.ADALError;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.cache.CacheKey;
import com.microsoft.identity.common.adal.internal.cache.DateTimeAdapter;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

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
public class ADALOAuth2TokenCache extends OAuth2TokenCache implements IShareSingleSignOnState {

    SharedPreferencesFileManager mSharedPreferencesFileManager;
    final static String SHARED_PREFERENCES_FILENAME = "com.microsoft.aad.adal.cache";
    private static final String TAG = "ADALOAuth2TokenCache";
    @SuppressLint("StaticFieldLeak")
    private static StorageHelper sHelper;
    private static final Object LOCK = new Object();
    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    private List<IShareSingleSignOnState> mSharedSSOCaches;


    public ADALOAuth2TokenCache(Context context, List<IShareSingleSignOnState> sharedSSOCaches) {
        super(context);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = sharedSSOCaches;
    }

    public ADALOAuth2TokenCache(Context context) {
        super(context);
        validateSecretKeySetting();
        initializeSharedPreferencesFileManager(ADALOAuth2TokenCache.SHARED_PREFERENCES_FILENAME);
        mSharedSSOCaches = new ArrayList<>();
    }

    protected void initializeSharedPreferencesFileManager(String fileName) {
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(super.mContext, fileName);
    }

    /**
     * Method responsible for saving tokens contained in the TokenResponse to storage.
     *
     * @param strategy
     * @param request
     * @param response
     */
    @Override
    public void saveTokens(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {

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

        while (otherCaches.hasNext()) {
            otherCaches.next().setSingleSignOnState(account, refreshToken);
        }

        // TODO: I'd like to know exactly why this is here before I put this back in.... i'm assuming for ADFS v3.
        //setItemToCacheForUser(resource, clientId, result, null);
    }


    private void setItemToCacheForUser(final String issuer, final String resource, final String clientId, final ADALTokenCacheItem cacheItem, final String userId) {

        setItem(CacheKey.createCacheKeyForRTEntry(issuer, resource, clientId, userId), cacheItem);

        if (cacheItem.getIsMultiResourceRefreshToken()) {
            setItem(CacheKey.createCacheKeyForMRRT(issuer, clientId, userId), cacheItem);
        }

        if (!StringExtensions.isNullOrBlank(cacheItem.getFamilyClientId())) {
            setItem(CacheKey.createCacheKeyForFRT(issuer, cacheItem.getFamilyClientId(), userId), cacheItem);
        }

    }

    private void setItem(String key, ADALTokenCacheItem cacheItem) {

        String json = mGson.toJson(cacheItem);
        String encrypted = encrypt(json);
        if (encrypted != null) {
            mSharedPreferencesFileManager.putString(key, encrypted);
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
            Log.e(TAG, ADALError.ENCRYPTION_ERROR.toString(), e);
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
            Log.e(TAG, ADALError.DECRYPTION_FAILED.toString(), e);
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
    public void setSingleSignOnState(Account account, RefreshToken refreshToken) {

    }

    @Override
    public RefreshToken getSingleSignOnState(Account account) {
        return null;
    }
}
