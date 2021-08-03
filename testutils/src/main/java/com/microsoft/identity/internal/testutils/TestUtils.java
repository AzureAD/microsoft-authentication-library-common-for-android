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
package com.microsoft.identity.internal.testutils;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.microsoft.identity.common.AndroidCommonComponents;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.java.interfaces.ICommonComponents;
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.dto.CredentialType;

import java.util.Map;

public class TestUtils {

    private static final Gson gson = new Gson();

    private static String getCacheKeyForAccessToken(Map<String, ?> cacheValues) {
        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccessToken(cacheKey)) {
                return cacheKey;
            }
        }

        return null;
    }

    public static boolean isAccessToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken;
    }

    public static boolean isRefreshToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.RefreshToken;
    }

    public static ISharedPreferencesFileManager getSharedPreferences(final String sharedPrefName) {
        final ICommonComponents components = new AndroidCommonComponents(ApplicationProvider.getApplicationContext());

        return components.getFileStore(sharedPrefName);
    }

    /**
     * Return a SharedPreferences instance that works with stores containing encrypted values.
     *
     * @param sharedPrefName the name of the shared preferences file.
     * @return A SharedPreferences that decrypts and encrypts the values.
     */
    public static ISharedPreferencesFileManager getEncryptedSharedPreferences(final String sharedPrefName) {
        final ICommonComponents components = new AndroidCommonComponents(ApplicationProvider.getApplicationContext());
        final ISharedPreferencesFileManager barePreferences = components.getEncryptedFileStore(
                sharedPrefName,
                components.
                        getStorageEncryptionManager());
        return barePreferences;
    }

    public static void clearCache(final String sharedPrefName) {
        ISharedPreferencesFileManager sharedPreferences = getSharedPreferences(sharedPrefName);
        sharedPreferences.clear();
    }

    public static void removeAccessTokenFromCache(final String sharedPrefName) {
        ISharedPreferencesFileManager sharedPreferences = getSharedPreferences(sharedPrefName);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        final String keyToRemove = getCacheKeyForAccessToken(cacheValues);
        if (keyToRemove != null) {
            sharedPreferences.remove(keyToRemove);
        }
    }

    public static Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

}
