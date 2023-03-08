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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.UTF8KeyAccessorStringAdapter;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;
import com.microsoft.identity.common.logging.Logger;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Convenience class for accessing {@link SharedPreferences}.
 */
public class SharedPreferencesFileManager implements IMultiTypeNameValueStorage {

    private static final String TAG = SharedPreferencesFileManager.class.getSimpleName();

    private final Object cacheLock = new Object();
    @GuardedBy("cacheLock")
    private final LruCache<String, String> fileCache = new LruCache<>(256);
    @GuardedBy("cacheLock")
    private final SharedPreferences mSharedPreferences;
    private final UTF8KeyAccessorStringAdapter mEncryptionManager;
    @VisibleForTesting
    private final String mSharedPreferencesFileName;
    // This is making a huge assumption - that we don't need to separate this cache by context.
    private static final ConcurrentMap<String, SharedPreferencesFileManager> objectCache =
            new ConcurrentHashMap<String, SharedPreferencesFileManager>(16, 0.75f, 1);


    /**
     * Constructs an instance of SharedPreferencesFileManager. Operating mode is always MODE_PRIVATE.
     *
     * @param context           Interface to global information about an application environment.
     * @param name              The desired {@link android.content.SharedPreferences} file. It will be created if it does not exist.
     * @param encryptionManager The {@link IKeyAccessor} to handle encryption/decryption of values.
     * @return The SharedPreferencesFileManager instance.
     */
    public static SharedPreferencesFileManager getSharedPreferences(final Context context,
                                                                    final String name,
                                                                    final IKeyAccessor encryptionManager) {
        String key = name + "/" + context.getPackageName() + "/" + Context.MODE_PRIVATE +
                "/" + ((encryptionManager == null) ? "clear" : encryptionManager.getClass().getCanonicalName());
        SharedPreferencesFileManager cachedFileManager = objectCache.get(key);
        if (cachedFileManager == null) {
            cachedFileManager = objectCache.putIfAbsent(key,
                    new SharedPreferencesFileManager(context, name, encryptionManager));
            if (cachedFileManager == null) {
                cachedFileManager = objectCache.get(key);
            }
        }
        return cachedFileManager;
    }

    /**
     * This method clears the singleton cache in use by the system, in the case that unsafe operations
     * have been performed on disk and the actual data needs to be removed.
     */
    public static void clearSingletonCache() {
        objectCache.clear();
    }

    /**
     * Constructs an instance of SharedPreferencesFileManager.
     * The default operating mode is {@link Context#MODE_PRIVATE}
     *
     * @param context           Interface to global information about an application environment.
     * @param name              The desired {@link android.content.SharedPreferences} file. It will be created
     *                          if it does not exist.
     * @param encryptionManager The {@link IKeyAccessor} to handle encryption/decryption of values.
     */
    public SharedPreferencesFileManager(
            final Context context,
            final String name,
            final IKeyAccessor encryptionManager) {
        if (encryptionManager == null) {
            Logger.verbose(TAG, "Init: ");
        } else {
            Logger.verbose(TAG, "Init with storage helper:  " + TAG);
        }
        mSharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        mSharedPreferencesFileName = name;

        if (encryptionManager != null) {
            mEncryptionManager = new UTF8KeyAccessorStringAdapter(encryptionManager);
        } else {
            mEncryptionManager = null;
        }
    }

    public final String getSharedPreferencesFileName() {
        return mSharedPreferencesFileName;
    }

    @Override
    public final void putString(
            final String key,
            final String value) {
        synchronized (cacheLock) {
            if (value != null) {
                fileCache.put(key, value);
            } else {
                fileCache.remove(key);
            }
            final SharedPreferences.Editor editor = mSharedPreferences.edit();

            if (null == mEncryptionManager || StringUtil.isNullOrEmpty(value)) {
                editor.putString(key, value);
            } else {
                final String encryptedValue = encrypt(value);
                editor.putString(key, encryptedValue);
            }

            editor.apply();
        }
    }

    private String getStringInternal(final String key, String valueFromSharedPrefs)
    {
        // Check the cache for the (potentially decrypted) value from the cache, and return
        synchronized (cacheLock) {
            String memCache = fileCache.get(key);
            if (memCache != null) {
                return memCache;
            }
        }

        // Only lookup a value if one was not provided.  Calls like 'getAll' already know the value
        // and only use this function for caching and decryption.
        if (valueFromSharedPrefs == null)
        {
            valueFromSharedPrefs = mSharedPreferences.getString(key, null);
        }

        String value = valueFromSharedPrefs;
        if (null != mEncryptionManager && !StringUtil.isNullOrEmpty(value)) {
            value = decrypt(value);

            if (StringUtil.isNullOrEmpty(value)) {
                logWarningAndRemoveKey(key);
            }
        }

        // Write back any (potentially decrypted) value from shared prefs read into the cache,
        // the same way we would on an explicit write.
        //
        // This will also check the cache under the lock one more time, so any new values that came in while
        // decryption was happening outside the lock will still win over our SharedPreferences-based value.    
        synchronized (cacheLock) {
            String memCache = fileCache.get(key);
            if (memCache != null) {
                return memCache;
            }
            if (!StringUtil.isNullOrEmpty(value)) {
                fileCache.put(key, value);
            }
        }

        return value;
    }

    @Override
    @Nullable
    public final String getString(final String key) {
        return getStringInternal(key, null);
    }

    @Override
    public void putLong(final String key, final long value) {
        putString(key, String.valueOf(value));
    }

    @Override
    public long getLong(final String key) {
        final String result = getString(key);

        if (!StringUtil.isNullOrEmpty(result)) {
            return Long.parseLong(result);
        }

        return 0;
    }

    private void logWarningAndRemoveKey(String key) {
        final String methodTag = TAG + ":logWarningAndRemoveKey";
        Logger.warn(
                methodTag,
                "Failed to decrypt value! "
                        + "This usually signals an issue with KeyStore or the provided SecretKeys."
        );

        remove(key);
    }

    @Override
    public final Map<String, String> getAll() {
        // We're not synchronizing this access, since we're not modifying it here.
        // Suppressing unchecked warnings due to casting Map<String,?> to Map<String,String>
        @SuppressWarnings(WarningType.unchecked_warning) final Map<String, String> entries = (Map<String, String>) mSharedPreferences.getAll();

        if (null != mEncryptionManager) {
            final Iterator<Map.Entry<String, String>> iterator = entries.entrySet().iterator();

            while (iterator.hasNext()) {
                final Map.Entry<String, String> entry = iterator.next();
                //This is slightly wasteful, but we have no better key iterator and decryption
                //is probably more painful than the additional file read when we miss in the cache.
                String decryptedValue = getStringInternal(entry.getKey(), entry.getValue());
                if (!StringUtil.isNullOrEmpty(decryptedValue)) {
                    entry.setValue(decryptedValue);
                }
            }
        }
        return entries;
    }

    @Override
    public final Iterator<Map.Entry<String, String>> getAllFilteredByKey(final @NonNull Predicate<String> keyFilter) {
        // We're not synchronizing this access, since we're not modifying it here.
        // Suppressing unchecked warnings due to casting Map<String,?> to Map<String,String>
        @SuppressWarnings(WarningType.unchecked_warning) final Map<String, String> entries = (Map<String, String>) mSharedPreferences.getAll();

        return new Iterator<Map.Entry<String, String>>() {
            final Iterator<Map.Entry<String, String>> iterator = entries.entrySet().iterator();
            Map.Entry<String, String> nextEntry = null;

            @Override
            public boolean hasNext() {
                if (nextEntry != null) {
                    return true;
                }
                if (!iterator.hasNext()) {
                    return false;
                }
                do {
                    Map.Entry<String, String> nextElement = iterator.next();
                    if (keyFilter.test(nextElement.getKey())) {
                        if (mEncryptionManager != null) {
                            String decryptedValue = getStringInternal(nextElement.getKey(), nextElement.getValue());
                            if (!StringUtil.isNullOrEmpty(decryptedValue)) {
                                nextEntry = new AbstractMap.SimpleEntry<String, String>(nextElement.getKey(), decryptedValue);
                            }
                        } else {
                            nextEntry = nextElement;
                        }
                    }
                } while (nextEntry == null && iterator.hasNext());
                return nextEntry != null;
            }

            @Override
            public Map.Entry<String, String> next() {
                if (nextEntry == null && !hasNext()) {
                    throw new NoSuchElementException();
                }
                final Map.Entry<String, String> tmp = nextEntry;
                nextEntry = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Removal is not supported");
            }
        };
    }


    @Override
    public final boolean contains(final String key) {
        return !StringUtil.isNullOrEmpty(getString(key));
    }

    @Override
    public final void clear() {
        synchronized (cacheLock) {
            final SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.clear();
            fileCache.evictAll();
            editor.apply();
        }
    }

    @Override
    public void remove(final String key) {
        final String methodTag = TAG + ":remove";
        Logger.info(
                methodTag,
                "Removing cache key"
        );
        synchronized (cacheLock) {
            fileCache.remove(key);
            final SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.remove(key);
            editor.apply();
        }

        Logger.infoPII(
                methodTag,
                "Removed cache key ["
                        + key
                        + "]"
        );
    }

    @Nullable
    private String encrypt(@NonNull final String clearText) {
        return encryptDecryptInternal(clearText, true);
    }

    @Nullable
    private String decrypt(@NonNull final String encryptedBlob) {
        return encryptDecryptInternal(encryptedBlob, false);
    }

    @Nullable
    private String encryptDecryptInternal(
            @NonNull final String inputText,
            final boolean encrypt) {
        final String methodTag = TAG + ":encryptDecryptInternal";

        String result;
        try {
            result = encrypt
                    ? mEncryptionManager.encrypt(inputText)
                    : mEncryptionManager.decrypt(inputText);
        } catch (ClientException e) {
            Logger.error(
                    methodTag,
                    "Failed to " + (encrypt ? "encrypt" : "decrypt") + " value",
                    encrypt
                            ? null // If we failed to encrypt, don't log the error as it may contain a token
                            : e // If we failed to decrypt, we couldn't see that secret value so log the error
            );

            // TODO determine if an Exception should be thrown here...
            result = null;
        }

        return result;
    }

}
