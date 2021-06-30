package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;

public class MsalOtherCache {

    /**
     * The name of the SharedPreferences file on disk.
     * This file is only used to store MSAL CPP authority validation metadata
     */
    public static final String DEFAULT_CPP_AUTHORITY_VALIDATION_METADATA_SHARED_PREFERENCES =
            "com.microsoft.identity.client.cpp_authority_validation_metadata";

    private final ICacheKeyValueDelegate mCacheKeyValueDelegate;

    private final ISharedPreferencesFileManager mCppAuthorityValidationMetadataSharedPreferencesFileManager;

    public MsalOtherCache(final Context context)
    {
        mCacheKeyValueDelegate = new CacheKeyValueDelegate();

        final IStorageHelper storageHelper = new StorageHelper(context);
        mCppAuthorityValidationMetadataSharedPreferencesFileManager =
                SharedPreferencesFileManager.getSharedPreferences(
                        context,
                        DEFAULT_CPP_AUTHORITY_VALIDATION_METADATA_SHARED_PREFERENCES,
                        storageHelper
                );
    }

    /**
     * This function is only used in the authority validation in MSAL CPP
     * Save the key/cacheValue to the cache.
     *
     * @param environment Environment.
     * @param cacheValue Value.
     */
    public void saveAuthorityValidationMetadata(final String environment, final String cacheValue)
    {
        final String cacheKey = mCacheKeyValueDelegate.generateAuthorityValidationMetadataKey(environment);
        mCppAuthorityValidationMetadataSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    /**
     * This function is only used in the authority validation in MSAL CPP
     * Returns the saved string value from the cache.
     *
     * @param environment Environment.
     * @return The string of the cached value, or null if not exist.
     */
    public String getAuthorityValidationMetadata(final String environment)
    {
        final String cacheKey = mCacheKeyValueDelegate.generateAuthorityValidationMetadataKey(environment);
        return mCppAuthorityValidationMetadataSharedPreferencesFileManager.getString(cacheKey);
    }

    /**
     * API to clear all cache.
     * Note: This method is intended to be only used for testing purposes.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public synchronized void clearCache() {
        mCppAuthorityValidationMetadataSharedPreferencesFileManager.clear();
    }
}
